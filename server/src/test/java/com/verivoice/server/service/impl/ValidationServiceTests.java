package com.verivoice.server.service.impl;

import com.verivoice.server.embeddable.ExtractedData;
import com.verivoice.server.entity.GstCache;
import com.verivoice.server.entity.Vendor;
import com.verivoice.server.entity.Document;
import com.verivoice.server.inspection.DocumentInspection;
import com.verivoice.server.repository.DocumentRepository;
import com.verivoice.server.repository.GoodsReceiptRepository;
import com.verivoice.server.repository.GstCacheRepository;
import com.verivoice.server.repository.PaymentRecordRepository;
import com.verivoice.server.repository.PurchaseOrderRepository;
import com.verivoice.server.repository.VendorRepository;
import com.verivoice.server.verification.CheckStatus;
import com.verivoice.server.verification.VerificationClassification;
import com.verivoice.server.verification.VerificationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationServiceTests {
    private DocumentRepository documentRepository;
    private GstCacheRepository gstCacheRepository;
    private VendorRepository vendorRepository;
    private PurchaseOrderRepository purchaseOrderRepository;
    private GoodsReceiptRepository goodsReceiptRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        gstCacheRepository = mock(GstCacheRepository.class);
        vendorRepository = mock(VendorRepository.class);
        purchaseOrderRepository = mock(PurchaseOrderRepository.class);
        goodsReceiptRepository = mock(GoodsReceiptRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        validationService = new ValidationService(
                documentRepository,
                gstCacheRepository,
                vendorRepository,
                purchaseOrderRepository,
                goodsReceiptRepository,
                paymentRecordRepository,
                new QrPayloadService(),
                new IrpSignatureService("")
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

        assertThat(outcome.score()).isEqualTo(70);
        assertThat(outcome.classification()).isEqualTo(VerificationClassification.LOW_RISK);
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
                true, true, payload, false, false, false,
                2, false, false, false, false, List.of(), "abc123"
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
    void rejectsAnInvoiceAlreadyRecordedAsPaid() {
        ExtractedData data = validInvoice();
        when(paymentRecordRepository
                .existsByVendorGstinIgnoreCaseAndInvoiceNumberIgnoreCase(
                        data.getGstNumber(),
                        data.getInvoiceNumber()
                ))
                .thenReturn(true);

        VerificationOutcome outcome = validationService.validate(data);

        assertThat(outcome.checks())
                .filteredOn(check -> "NOT_ALREADY_PAID".equals(check.getCode()))
                .singleElement()
                .extracting(check -> check.getStatus())
                .isEqualTo(CheckStatus.FAILED);
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
