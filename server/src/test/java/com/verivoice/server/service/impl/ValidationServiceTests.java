package com.verivoice.server.service.impl;

import com.verivoice.server.embeddable.ExtractedData;
import com.verivoice.server.entity.GstCache;
import com.verivoice.server.entity.Vendor;
import com.verivoice.server.entity.Document;
import com.verivoice.server.inspection.DocumentInspection;
import com.verivoice.server.repository.DocumentRepository;
import com.verivoice.server.repository.GstCacheRepository;
import com.verivoice.server.repository.VendorRepository;
import com.verivoice.server.verification.CheckStatus;
import com.verivoice.server.verification.VerificationClassification;
import com.verivoice.server.verification.VerificationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidationServiceTests {
    private DocumentRepository documentRepository;
    private GstCacheRepository gstCacheRepository;
    private VendorRepository vendorRepository;
    private FreeGstinVerificationService freeGstinVerificationService;
    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        gstCacheRepository = mock(GstCacheRepository.class);
        vendorRepository = mock(VendorRepository.class);
        freeGstinVerificationService = mock(FreeGstinVerificationService.class);
        validationService = new ValidationService(
                documentRepository,
                gstCacheRepository,
                vendorRepository,
                new QrPayloadService(),
                freeGstinVerificationService
        );

    }

    @Test
    void awardsOnlyPointsBackedByPerformedChecks() {
        ExtractedData data = validInvoice();
        GstCache cache = new GstCache(
                data.getGstNumber(),
                "Acme Private Limited",
                "ACTIVE",
                LocalDateTime.now()
        );
        Vendor vendor = new Vendor(
                data.getGstNumber(),
                "Acme Private Limited",
                "Acme",
                "Karnataka",
                "TRUSTED",
                LocalDateTime.now()
        );

        when(gstCacheRepository.findById(data.getGstNumber())).thenReturn(Optional.of(cache));
        when(vendorRepository.findById(data.getGstNumber())).thenReturn(Optional.of(vendor));
        when(documentRepository.existsByExtractedDataGstNumber(data.getGstNumber())).thenReturn(true);

        VerificationOutcome outcome = validationService.validate(data);


        assertThat(outcome.score()).isEqualTo(68); // 68: GST checks (50) + VENDOR_HISTORY checks (8)

        assertThat(outcome.classification()).isEqualTo(VerificationClassification.REVIEW_REQUIRED);


        assertThat(outcome.checks())
                .filteredOn(check -> "SIGNATURE_VALID".equals(check.getCode()))
                .singleElement()

                .extracting(check -> check.getStatus())
                .isEqualTo(CheckStatus.NOT_PERFORMED);
    }

    @Test
    void flagsBothSupportedDuplicateSignatures() {
        ExtractedData data = validInvoice();
        when(gstCacheRepository.findById(data.getGstNumber())).thenReturn(Optional.empty());
        when(vendorRepository.findById(data.getGstNumber())).thenReturn(Optional.empty());
        when(documentRepository
                .existsByExtractedDataGstNumberAndExtractedDataInvoiceNumber(
                        data.getGstNumber(),
                        data.getInvoiceNumber()
                ))
                .thenReturn(true);
        when(documentRepository
                .existsByExtractedDataGstNumberAndExtractedDataTotalAmountAndExtractedDataInvoiceDate(
                        data.getGstNumber(),
                        data.getTotalAmount(),
                        data.getInvoiceDate()
                ))
                .thenReturn(true);

        VerificationOutcome outcome = validationService.validate(data);

        assertThat(outcome.checks())
                .filteredOn(check -> "DUPLICATE_DETECTION".equals(check.getLayer())
                        && check.getStatus() == CheckStatus.FAILED)
                .hasSize(3);
        assertThat(outcome.score()).isEqualTo(10);
        assertThat(outcome.classification()).isEqualTo(VerificationClassification.HIGH_RISK);
    }

    @Test
    void missingExtractionProducesAHighRiskResult() {
        VerificationOutcome outcome = validationService.validate(null);

        assertThat(outcome.score()).isZero();
        assertThat(outcome.classification()).isEqualTo(VerificationClassification.HIGH_RISK);
        assertThat(outcome.anomalies()).containsExactly(
                "Invoice fields extracted: No structured invoice data was extracted."
        );
    }

    @Test
    void detectsInvoiceDataThatDoesNotMatchDecodedQr() {
        ExtractedData data = validInvoice();
        String payload = """
                {
                  "SellerGstin":"29ABCDE1234F1Z5",
                  "DocNo":"INV-9999",
                  "DocDt":"18/06/2026",
                  "TotInvVal":9999,
                  "Irn":"official-irn"
                }
                """;
        DocumentInspection inspection = new DocumentInspection(
                true, payload, "abc123"
        );

        VerificationOutcome outcome = validationService.validate(data, inspection);

        assertThat(outcome.checks())
                .filteredOn(check -> "PAYLOAD_MATCH".equals(check.getCode()))
                .singleElement()
                .satisfies(check -> {
                    assertThat(check.getStatus()).isEqualTo(CheckStatus.FAILED);
                    assertThat(check.getDetail()).contains("invoice number", "total amount");
                });
        assertThat(data.getIrn()).isEqualTo("official-irn");
    }

    @Test
    void doesNotVerifyMalformedGstinAgainstRegistry() {
        ExtractedData data = validInvoice();
        data.setGstNumber("29INVALID");

        VerificationOutcome outcome = validationService.validate(data);

        assertThat(outcome.checks())
                .filteredOn(check -> "GST_STATUS".equals(check.getCode()))
                .singleElement()
                .extracting(check -> check.getStatus())
                .isEqualTo(CheckStatus.NOT_PERFORMED);
    }

    @Test
    void doesNotTrustExpiredGstCache() {
        ExtractedData data = validInvoice();
        GstCache expired = new GstCache(
                data.getGstNumber(), "Acme Private Limited", "ACTIVE", LocalDateTime.now().minusDays(2)
        );
        when(gstCacheRepository.findById(data.getGstNumber())).thenReturn(Optional.of(expired));

        VerificationOutcome outcome = validationService.validate(data);

        assertThat(outcome.checks())
                .filteredOn(check -> "GST_STATUS".equals(check.getCode()))
                .singleElement()
                .extracting(check -> check.getStatus())
                .isEqualTo(CheckStatus.NOT_PERFORMED);
    }

    @Test
    void detectsBackwardsInvoiceSequenceForNewerInvoiceDate() {
        ExtractedData data = validInvoice();
        data.setInvoiceNumber("INV-1000");
        Document previous = new Document();
        ExtractedData previousData = validInvoice();
        previousData.setInvoiceNumber("INV-1050");
        previousData.setInvoiceDate(LocalDate.of(2026, 6, 17));
        previous.setExtractedData(previousData);
        when(documentRepository
                .findTopByExtractedDataGstNumberOrderByExtractedDataInvoiceDateDescUploadDateDesc(
                        data.getGstNumber()
                ))
                .thenReturn(Optional.of(previous));

        VerificationOutcome outcome = validationService.validate(data);

        assertThat(outcome.checks())
                .filteredOn(check -> "INVOICE_SEQUENCE".equals(check.getCode()))
                .singleElement()
                .extracting(check -> check.getStatus())
                .isEqualTo(CheckStatus.FAILED);
    }

    @Test
    void marksValidFormatButFakeGstinAsFailedAfterLiveLookup() {
        ExtractedData data = validInvoice();
        data.setGstNumber("27ABCDE1234F1Z5");

        when(gstCacheRepository.findById(data.getGstNumber())).thenReturn(Optional.empty());
        when(freeGstinVerificationService.verifyAndCache(data.getGstNumber())).thenReturn(Optional.of(
                new FreeGstinVerificationService.GstinVerifyResult(false, null, null, "ref-123")
        ));

        VerificationOutcome outcome = validationService.validate(data);

        assertThat(outcome.checks())
                .filteredOn(check -> "GST_STATUS".equals(check.getCode()))
                .singleElement()
                .satisfies(check -> {
                    assertThat(check.getStatus()).isEqualTo(CheckStatus.FAILED);
                    assertThat(check.getDetail()).contains("invalid or not found");
                });

        assertThat(outcome.checks())
                .filteredOn(check -> "LEGAL_NAME_MATCH".equals(check.getCode()))
                .singleElement()
                .extracting(check -> check.getStatus())
                .isEqualTo(CheckStatus.NOT_PERFORMED);

        verify(freeGstinVerificationService).verifyAndCache(data.getGstNumber());
    }

    private ExtractedData validInvoice() {
        ExtractedData data = new ExtractedData();
        data.setVendorName("Acme Pvt Ltd");
        data.setInvoiceNumber("INV-1001");
        data.setGstNumber("29ABCDE1234F1Z5");
        data.setSubtotal(1000d);
        data.setTaxAmount(180d);
        data.setCgstAmount(90d);
        data.setSgstAmount(90d);
        data.setGstRate(18d);
        data.setTotalAmount(1180d);
        data.setInvoiceDate(LocalDate.of(2026, 6, 18));
        data.setQrCode("decoded-payload");
        data.setIrn("sample-irn");
        return data;
    }
}
