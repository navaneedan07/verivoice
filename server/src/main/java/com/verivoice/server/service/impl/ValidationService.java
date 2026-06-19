package com.verivoice.server.service.impl;

import com.verivoice.server.embeddable.ExtractedData;
import com.verivoice.server.entity.GstCache;
import com.verivoice.server.entity.Vendor;
import com.verivoice.server.entity.Document;
import com.verivoice.server.erp.PurchaseOrder;
import com.verivoice.server.inspection.DocumentInspection;
import com.verivoice.server.inspection.QrPayloadData;
import com.verivoice.server.inspection.SignatureVerification;
import com.verivoice.server.repository.DocumentRepository;
import com.verivoice.server.repository.GoodsReceiptRepository;
import com.verivoice.server.repository.GstCacheRepository;
import com.verivoice.server.repository.PaymentRecordRepository;
import com.verivoice.server.repository.PurchaseOrderRepository;
import com.verivoice.server.repository.VendorRepository;
import com.verivoice.server.verification.CheckStatus;
import com.verivoice.server.verification.VerificationCheck;
import com.verivoice.server.verification.VerificationClassification;
import com.verivoice.server.verification.VerificationOutcome;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.Objects;

@Service
public class ValidationService {
    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final double MONEY_TOLERANCE = 0.01d;

    private final DocumentRepository documentRepository;
    private final GstCacheRepository gstCacheRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final QrPayloadService qrPayloadService;
    private final IrpSignatureService irpSignatureService;

    public ValidationService(
            DocumentRepository documentRepository,
            GstCacheRepository gstCacheRepository,
            VendorRepository vendorRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            GoodsReceiptRepository goodsReceiptRepository,
            PaymentRecordRepository paymentRecordRepository,
            QrPayloadService qrPayloadService,
            IrpSignatureService irpSignatureService
    ) {
        this.documentRepository = documentRepository;
        this.gstCacheRepository = gstCacheRepository;
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.qrPayloadService = qrPayloadService;
        this.irpSignatureService = irpSignatureService;
    }

    public VerificationOutcome validate(ExtractedData data) {
        return validate(data, DocumentInspection.unsupported());
    }

    public VerificationOutcome validate(ExtractedData data, DocumentInspection inspection) {
        List<VerificationCheck> checks = new ArrayList<>();

        if (data == null) {
            checks.add(failed("DOCUMENT_PARSING", "EXTRACTION", "Invoice fields extracted",
                    "No structured invoice data was extracted."));
            return outcome(checks);
        }

        String gstin = normalizeGstin(data.getGstNumber());
        Optional<GstCache> cachedGst = gstin == null
                ? Optional.empty()
                : gstCacheRepository.findById(gstin);

        addGstChecks(checks, data, gstin, cachedGst);
        addQrChecks(checks, data, inspection);
        addMathChecks(checks, data);
        addFraudChecks(checks, data, inspection);
        addVendorHistoryChecks(checks, gstin);
        addDuplicateChecks(checks, data, gstin, inspection);
        addInvoiceSequenceChecks(checks, data, gstin);
        addErpChecks(checks, data, gstin);

        return outcome(checks);
    }

    private void addGstChecks(
            List<VerificationCheck> checks,
            ExtractedData data,
            String gstin,
            Optional<GstCache> cachedGst
    ) {
        if (gstin == null) {
            checks.add(failed("GST_VERIFICATION", "GSTIN_PRESENT", "GSTIN exists",
                    "GSTIN is missing."));
            checks.add(notPerformed("GST_VERIFICATION", "GST_STATUS", "GST status active",
                    "Cannot query GST status without a GSTIN."));
            checks.add(notPerformed("GST_VERIFICATION", "LEGAL_NAME_MATCH", "Legal name matches",
                    "Cannot compare the legal name without GST registry data."));
            checks.add(notPerformed("GST_VERIFICATION", "STATE_CODE", "State code matches",
                    "Cannot validate the state code without a GSTIN."));
            return;
        }

        boolean validFormat = GSTIN_PATTERN.matcher(gstin).matches();
        checks.add(validFormat
                ? passed("GST_VERIFICATION", "GSTIN_FORMAT", "GSTIN format valid",
                "GSTIN matches the statutory 15-character format.", 0)
                : failed("GST_VERIFICATION", "GSTIN_FORMAT", "GSTIN format valid",
                "GSTIN does not match the statutory 15-character format."));

        if (cachedGst.isEmpty()) {
            checks.add(notPerformed("GST_VERIFICATION", "GST_STATUS", "GST status active",
                    "No verified GST registry response is available in the cache."));
            checks.add(notPerformed("GST_VERIFICATION", "LEGAL_NAME_MATCH", "Legal name matches",
                    "No verified GST registry legal name is available."));
        } else {
            GstCache gst = cachedGst.get();
            boolean active = "ACTIVE".equalsIgnoreCase(gst.getStatus());
            checks.add(active
                    ? passed("GST_VERIFICATION", "GST_STATUS", "GST status active",
                    "Cached GST registry status is active.", 30)
                    : failed("GST_VERIFICATION", "GST_STATUS", "GST status active",
                    "Cached GST registry status is " + gst.getStatus() + "."));

            boolean nameMatches = namesMatch(data.getVendorName(), gst.getLegalName());
            checks.add(nameMatches
                    ? passed("GST_VERIFICATION", "LEGAL_NAME_MATCH", "Legal name matches",
                    "Extracted vendor name matches the cached legal name.", 20)
                    : failed("GST_VERIFICATION", "LEGAL_NAME_MATCH", "Legal name matches",
                    "Extracted vendor name does not match the cached legal name."));
        }

        boolean stateCodeValid = validFormat && isValidStateCode(gstin.substring(0, 2));
        checks.add(stateCodeValid
                ? passed("GST_VERIFICATION", "STATE_CODE", "State code valid",
                "GSTIN begins with a recognized state or territory code.", 0)
                : failed("GST_VERIFICATION", "STATE_CODE", "State code valid",
                "GSTIN state code is missing or invalid."));
    }

    private void addQrChecks(
            List<VerificationCheck> checks,
            ExtractedData data,
            DocumentInspection inspection
    ) {
        boolean qrPresent = inspection.qrPresent() && hasText(inspection.qrPayload());
        checks.add(qrPresent
                ? passed("QR_IRN_VERIFICATION", "QR_PRESENT", "QR code present",
                "A QR code was found and decoded from the document pixels.", 0)
                : failed("QR_IRN_VERIFICATION", "QR_PRESENT", "QR code present",
                "No QR payload was extracted."));

        checks.add(qrPresent
                ? passed("QR_IRN_VERIFICATION", "QR_DECODED", "QR code decoded",
                "ZXing decoded the QR image.", 0)
                : notPerformed("QR_IRN_VERIFICATION", "QR_DECODED", "QR code decoded",
                "No QR payload is available to decode."));

        QrPayloadData qr = qrPayloadService.parse(inspection.qrPayload());
        String irn = hasText(qr.irn()) ? qr.irn() : data.getIrn();
        checks.add(hasText(irn)
                ? passed("QR_IRN_VERIFICATION", "IRN_PRESENT", "IRN present",
                "An invoice reference number was extracted.", 0)
                : failed("QR_IRN_VERIFICATION", "IRN_PRESENT", "IRN present",
                "No invoice reference number was extracted."));

        SignatureVerification signature = irpSignatureService.verify(inspection.qrPayload());
        checks.add(switch (signature.status()) {
            case VERIFIED -> passed(
                    "QR_IRN_VERIFICATION", "SIGNATURE_VALID", "Digital signature valid",
                    signature.detail(), 30
            );
            case INVALID -> failed(
                    "QR_IRN_VERIFICATION", "SIGNATURE_VALID", "Digital signature valid",
                    signature.detail()
            );
            case NOT_CONFIGURED, UNSUPPORTED -> notPerformed(
                    "QR_IRN_VERIFICATION", "SIGNATURE_VALID", "Digital signature valid",
                    signature.detail()
            );
        });

        if (!qr.structured()) {
            checks.add(notPerformed("QR_IRN_VERIFICATION", "PAYLOAD_MATCH", "Invoice matches QR payload",
                    "The QR was decoded, but its signed payload format could not be parsed."));
        } else {
            List<String> mismatches = compareQr(data, qr);
            checks.add(mismatches.isEmpty()
                    ? passed("QR_IRN_VERIFICATION", "PAYLOAD_MATCH", "Invoice matches QR payload",
                    "Seller GSTIN, invoice number, date, and amount match the decoded QR fields.", 0)
                    : failed("QR_IRN_VERIFICATION", "PAYLOAD_MATCH", "Invoice matches QR payload",
                    "Mismatched QR fields: " + String.join(", ", mismatches) + "."));
        }
    }

    private void addMathChecks(List<VerificationCheck> checks, ExtractedData data) {
        if (data.getSubtotal() == null || data.getTaxAmount() == null || data.getTotalAmount() == null) {
            checks.add(notPerformed("MATHEMATICAL_AUDIT", "TOTAL_RECONCILES", "Subtotal + tax = total",
                    "Subtotal, tax amount, and total are all required."));
        } else {
            boolean reconciles = nearlyEqual(
                    data.getSubtotal() + data.getTaxAmount(),
                    data.getTotalAmount()
            );
            checks.add(reconciles
                    ? passed("MATHEMATICAL_AUDIT", "TOTAL_RECONCILES", "Subtotal + tax = total",
                    "Invoice totals reconcile within one paisa.", 10)
                    : failed("MATHEMATICAL_AUDIT", "TOTAL_RECONCILES", "Subtotal + tax = total",
                    "Subtotal plus tax does not equal the invoice total."));
        }

        if (data.getCgstAmount() == null || data.getSgstAmount() == null || data.getTaxAmount() == null) {
            checks.add(notPerformed("MATHEMATICAL_AUDIT", "SPLIT_TAX_RECONCILES",
                    "CGST + SGST is correct", "CGST, SGST, and total tax are required."));
        } else {
            boolean reconciles = nearlyEqual(
                    data.getCgstAmount() + data.getSgstAmount(),
                    data.getTaxAmount()
            );
            checks.add(reconciles
                    ? passed("MATHEMATICAL_AUDIT", "SPLIT_TAX_RECONCILES", "CGST + SGST is correct",
                    "CGST and SGST reconcile to total tax.", 0)
                    : failed("MATHEMATICAL_AUDIT", "SPLIT_TAX_RECONCILES", "CGST + SGST is correct",
                    "CGST plus SGST does not equal total tax."));
        }

        if (data.getSubtotal() == null || data.getTaxAmount() == null || data.getGstRate() == null
                || data.getSubtotal() == 0) {
            checks.add(notPerformed("MATHEMATICAL_AUDIT", "GST_RATE_RECONCILES",
                    "GST percentage correct", "Subtotal, tax, and GST rate are required."));
        } else {
            double expectedTax = data.getSubtotal() * data.getGstRate() / 100d;
            checks.add(nearlyEqual(expectedTax, data.getTaxAmount())
                    ? passed("MATHEMATICAL_AUDIT", "GST_RATE_RECONCILES", "GST percentage correct",
                    "Declared GST rate reconciles to the tax amount.", 0)
                    : failed("MATHEMATICAL_AUDIT", "GST_RATE_RECONCILES", "GST percentage correct",
                    "Declared GST rate does not reconcile to the tax amount."));
        }

        checks.add(notPerformed("MATHEMATICAL_AUDIT", "HSN_RATE_VALID", "HSN/SAC rates valid",
                "An authoritative HSN/SAC tax-rate catalogue is not configured."));
    }

    private void addFraudChecks(
            List<VerificationCheck> checks,
            ExtractedData data,
            DocumentInspection inspection
    ) {
        if (!inspection.pdf()) {
            checks.add(notPerformed("FRAUD_ANALYSIS", "PDF_EDITED", "PDF edit history clean",
                    "The uploaded document is not a PDF."));
            checks.add(notPerformed("FRAUD_ANALYSIS", "FONT_CONSISTENCY", "Fonts consistent",
                    "Font inspection currently applies to PDF files."));
        } else {
            boolean editingSignals = inspection.metadataSuggestsEditing()
                    || inspection.multipleRevisions();
            checks.add(editingSignals
                    ? failed("FRAUD_ANALYSIS", "PDF_EDITED", "PDF edit history clean",
                    String.join(" ", inspection.forensicNotes()))
                    : passed("FRAUD_ANALYSIS", "PDF_EDITED", "PDF edit history clean",
                    "No metadata or incremental-revision edit signal was found.", 0));
            checks.add(inspection.suspiciousFontUsage()
                    ? failed("FRAUD_ANALYSIS", "FONT_CONSISTENCY", "Fonts consistent",
                    "The PDF uses " + inspection.fontCount() + " fonts, above the configured threshold.")
                    : passed("FRAUD_ANALYSIS", "FONT_CONSISTENCY", "Fonts consistent",
                    "PDF font count is within the configured threshold.", 0));
        }
        checks.add(!inspection.amountTypographyChecked()
                ? notPerformed("FRAUD_ANALYSIS", "AMOUNT_TAMPERING", "Amount typography consistent",
                "The total amount could not be located in extractable PDF text.")
                : inspection.amountTypographyAnomaly()
                ? failed("FRAUD_ANALYSIS", "AMOUNT_TAMPERING", "Amount typography consistent",
                "The invoice total is rendered in a rare font that differs from the dominant font.")
                : passed("FRAUD_ANALYSIS", "AMOUNT_TAMPERING", "Amount typography consistent",
                "No unusual font substitution was found around the invoice total.", 0));
        checks.add(inspection.imageOnlyPdf()
                ? failed("FRAUD_ANALYSIS", "LAYOUT", "Layout is not suspicious",
                "The PDF is image-only, which limits text-level forensic validation.")
                : passed("FRAUD_ANALYSIS", "LAYOUT", "Layout is not suspicious",
                "The document contains extractable text and no image-only signal.", 0));

        List<String> missing = new ArrayList<>();
        if (!hasText(data.getVendorName())) missing.add("vendor name");
        if (!hasText(data.getGstNumber())) missing.add("GSTIN");
        if (!hasText(data.getInvoiceNumber())) missing.add("invoice number");
        if (data.getInvoiceDate() == null) missing.add("invoice date");
        if (data.getTotalAmount() == null) missing.add("total amount");

        checks.add(missing.isEmpty()
                ? passed("FRAUD_ANALYSIS", "MANDATORY_FIELDS", "Mandatory fields present",
                "All core invoice fields are present.", 0)
                : failed("FRAUD_ANALYSIS", "MANDATORY_FIELDS", "Mandatory fields present",
                "Missing: " + String.join(", ", missing) + "."));
    }

    private void addVendorHistoryChecks(List<VerificationCheck> checks, String gstin) {
        if (gstin == null) {
            checks.add(notPerformed("VENDOR_HISTORY", "VENDOR_SEEN", "Vendor seen before",
                    "GSTIN is required for vendor history."));
            checks.add(notPerformed("VENDOR_HISTORY", "GST_PREVIOUSLY_VERIFIED", "GST verified before",
                    "GSTIN is required for verification history."));
            checks.add(notPerformed("VENDOR_HISTORY", "TRUSTED_VENDOR", "Known trusted vendor",
                    "GSTIN is required for trust history."));
            return;
        }

        Optional<Vendor> vendor = vendorRepository.findById(gstin);
        checks.add(documentRepository.existsByExtractedDataGstNumber(gstin)
                ? passed("VENDOR_HISTORY", "VENDOR_SEEN", "Vendor seen before",
                "A previous invoice exists for this GSTIN.", 0)
                : failed("VENDOR_HISTORY", "VENDOR_SEEN", "Vendor seen before",
                "No previous invoice exists for this GSTIN."));
        checks.add(vendor.filter(value -> value.getVerifiedAt() != null).isPresent()
                ? passed("VENDOR_HISTORY", "GST_PREVIOUSLY_VERIFIED", "GST verified before",
                "Vendor has a previous verification timestamp.", 0)
                : failed("VENDOR_HISTORY", "GST_PREVIOUSLY_VERIFIED", "GST verified before",
                "Vendor has not previously been verified."));
        checks.add(vendor.filter(value -> "TRUSTED".equalsIgnoreCase(value.getStatus())).isPresent()
                ? passed("VENDOR_HISTORY", "TRUSTED_VENDOR", "Known trusted vendor",
                "Vendor is marked trusted.", 0)
                : failed("VENDOR_HISTORY", "TRUSTED_VENDOR", "Known trusted vendor",
                "Vendor is not marked trusted."));
    }

    private void addDuplicateChecks(
            List<VerificationCheck> checks,
            ExtractedData data,
            String gstin,
            DocumentInspection inspection
    ) {
        if (inspection.sha256() == null) {
            checks.add(notPerformed("DUPLICATE_DETECTION", "UNIQUE_FILE",
                    "Source file is unique", "A source-file fingerprint is unavailable."));
        } else {
            boolean exactDuplicate = documentRepository.existsByFileHash(inspection.sha256());
            checks.add(exactDuplicate
                    ? failed("DUPLICATE_DETECTION", "UNIQUE_FILE", "Source file is unique",
                    "An identical SHA-256 document fingerprint already exists.")
                    : passed("DUPLICATE_DETECTION", "UNIQUE_FILE", "Source file is unique",
                    "No byte-identical uploaded file was found.", 0));
        }
        if (gstin == null || !hasText(data.getInvoiceNumber())) {
            checks.add(notPerformed("DUPLICATE_DETECTION", "UNIQUE_INVOICE_NUMBER",
                    "GSTIN + invoice number unique", "GSTIN and invoice number are required."));
        } else {
            boolean duplicate = documentRepository
                    .existsByExtractedDataGstNumberAndExtractedDataInvoiceNumber(
                            gstin,
                            data.getInvoiceNumber().trim()
                    );
            checks.add(duplicate
                    ? failed("DUPLICATE_DETECTION", "UNIQUE_INVOICE_NUMBER",
                    "GSTIN + invoice number unique", "A matching invoice already exists.")
                    : passed("DUPLICATE_DETECTION", "UNIQUE_INVOICE_NUMBER",
                    "GSTIN + invoice number unique", "No matching invoice number was found.", 0));
        }

        if (gstin == null || data.getTotalAmount() == null || data.getInvoiceDate() == null) {
            checks.add(notPerformed("DUPLICATE_DETECTION", "UNIQUE_AMOUNT_DATE",
                    "GSTIN + amount + date unique", "GSTIN, amount, and date are required."));
        } else {
            boolean duplicate = documentRepository
                    .existsByExtractedDataGstNumberAndExtractedDataTotalAmountAndExtractedDataInvoiceDate(
                            gstin,
                            data.getTotalAmount(),
                            data.getInvoiceDate()
                    );
            checks.add(duplicate
                    ? failed("DUPLICATE_DETECTION", "UNIQUE_AMOUNT_DATE",
                    "GSTIN + amount + date unique", "A matching amount and date already exist.")
                    : passed("DUPLICATE_DETECTION", "UNIQUE_AMOUNT_DATE",
                    "GSTIN + amount + date unique", "No matching amount and date were found.", 0));
        }

        boolean enoughData = gstin != null
                && hasText(data.getInvoiceNumber())
                && data.getTotalAmount() != null
                && data.getInvoiceDate() != null;
        boolean duplicateFound = checks.stream()
                .filter(check -> "DUPLICATE_DETECTION".equals(check.getLayer()))
                .anyMatch(check -> check.getStatus() == CheckStatus.FAILED);
        checks.add(!enoughData
                ? notPerformed("DUPLICATE_DETECTION", "NO_DUPLICATE", "No duplicate found",
                "All duplicate identifiers are required.")
                : duplicateFound
                ? failed("DUPLICATE_DETECTION", "NO_DUPLICATE", "No duplicate found",
                "At least one duplicate signature matched.")
                : passed("DUPLICATE_DETECTION", "NO_DUPLICATE", "No duplicate found",
                "Both duplicate signatures are unique.", 10));
    }

    private void addInvoiceSequenceChecks(
            List<VerificationCheck> checks,
            ExtractedData data,
            String gstin
    ) {
        if (gstin == null || !hasText(data.getInvoiceNumber()) || data.getInvoiceDate() == null) {
            checks.add(notPerformed("VENDOR_HISTORY", "INVOICE_SEQUENCE", "Invoice sequence plausible",
                    "GSTIN, invoice number, and invoice date are required."));
            return;
        }
        Optional<Document> latest = documentRepository
                .findTopByExtractedDataGstNumberOrderByExtractedDataInvoiceDateDescUploadDateDesc(gstin);
        if (latest.isEmpty()) {
            checks.add(notPerformed("VENDOR_HISTORY", "INVOICE_SEQUENCE", "Invoice sequence plausible",
                    "This is the first stored invoice for the vendor."));
            return;
        }

        Integer currentNumber = trailingNumber(data.getInvoiceNumber());
        ExtractedData previousData = latest.get().getExtractedData();
        Integer previousNumber = trailingNumber(previousData.getInvoiceNumber());
        if (currentNumber == null || previousNumber == null) {
            checks.add(notPerformed("VENDOR_HISTORY", "INVOICE_SEQUENCE", "Invoice sequence plausible",
                    "Invoice numbers do not end in comparable numeric sequences."));
            return;
        }

        boolean chronologicallyNewer = !data.getInvoiceDate().isBefore(previousData.getInvoiceDate());
        boolean backwards = chronologicallyNewer && currentNumber <= previousNumber;
        boolean extremeJump = chronologicallyNewer && currentNumber - previousNumber > 10_000;
        checks.add(backwards || extremeJump
                ? failed("VENDOR_HISTORY", "INVOICE_SEQUENCE", "Invoice sequence plausible",
                "Previous stored invoice was " + previousData.getInvoiceNumber()
                        + " dated " + previousData.getInvoiceDate() + ".")
                : passed("VENDOR_HISTORY", "INVOICE_SEQUENCE", "Invoice sequence plausible",
                "Invoice number is chronologically plausible relative to stored history.", 0));
    }

    private void addErpChecks(List<VerificationCheck> checks, ExtractedData data, String gstin) {
        if (!hasText(data.getPurchaseOrderNumber())) {
            checks.add(notPerformed("ERP_MATCHING", "PO_EXISTS", "Purchase order exists",
                    "No purchase order number was extracted."));
            checks.add(notPerformed("ERP_MATCHING", "VENDOR_EXISTS", "Vendor matches purchase order",
                    "No purchase order number was extracted."));
            checks.add(notPerformed("ERP_MATCHING", "PO_AMOUNT_MATCH", "Invoice amount matches PO",
                    "No purchase order number was extracted."));
            checks.add(notPerformed("ERP_MATCHING", "GRN_EXISTS", "Goods receipt note exists",
                    "No purchase order number was extracted."));
        } else {
            Optional<PurchaseOrder> order = purchaseOrderRepository
                    .findByPoNumberIgnoreCase(data.getPurchaseOrderNumber().trim());
            checks.add(order.isPresent()
                    ? passed("ERP_MATCHING", "PO_EXISTS", "Purchase order exists",
                    "Purchase order exists in the internal ledger.", 0)
                    : failed("ERP_MATCHING", "PO_EXISTS", "Purchase order exists",
                    "No matching purchase order exists in the internal ledger."));

            if (order.isPresent()) {
                PurchaseOrder po = order.get();
                boolean vendorMatches = gstin != null && gstin.equalsIgnoreCase(po.getVendorGstin());
                checks.add(vendorMatches
                        ? passed("ERP_MATCHING", "VENDOR_EXISTS", "Vendor matches purchase order",
                        "Invoice GSTIN matches the purchase order vendor.", 0)
                        : failed("ERP_MATCHING", "VENDOR_EXISTS", "Vendor matches purchase order",
                        "Invoice GSTIN does not match the purchase order vendor."));

                boolean amountMatches = data.getTotalAmount() != null
                        && po.getAmount() != null
                        && nearlyEqual(data.getTotalAmount(), po.getAmount().doubleValue());
                checks.add(amountMatches
                        ? passed("ERP_MATCHING", "PO_AMOUNT_MATCH", "Invoice amount matches PO",
                        "Invoice total matches the purchase order amount.", 0)
                        : failed("ERP_MATCHING", "PO_AMOUNT_MATCH", "Invoice amount matches PO",
                        "Invoice total does not match the purchase order amount."));

                boolean grnExists = goodsReceiptRepository
                        .existsByPoNumberIgnoreCaseAndAcceptedTrue(po.getPoNumber());
                checks.add(grnExists
                        ? passed("ERP_MATCHING", "GRN_EXISTS", "Goods receipt note exists",
                        "An accepted goods receipt exists for the purchase order.", 0)
                        : failed("ERP_MATCHING", "GRN_EXISTS", "Goods receipt note exists",
                        "No accepted goods receipt exists for the purchase order."));
            } else {
                checks.add(notPerformed("ERP_MATCHING", "VENDOR_EXISTS", "Vendor matches purchase order",
                        "Purchase order was not found."));
                checks.add(notPerformed("ERP_MATCHING", "PO_AMOUNT_MATCH", "Invoice amount matches PO",
                        "Purchase order was not found."));
                checks.add(notPerformed("ERP_MATCHING", "GRN_EXISTS", "Goods receipt note exists",
                        "Purchase order was not found."));
            }
        }

        if (gstin == null || !hasText(data.getInvoiceNumber())) {
            checks.add(notPerformed("ERP_MATCHING", "NOT_ALREADY_PAID", "Invoice not already paid",
                    "GSTIN and invoice number are required."));
        } else {
            boolean paid = paymentRecordRepository
                    .existsByVendorGstinIgnoreCaseAndInvoiceNumberIgnoreCase(
                            gstin,
                            data.getInvoiceNumber().trim()
                    );
            checks.add(paid
                    ? failed("ERP_MATCHING", "NOT_ALREADY_PAID", "Invoice not already paid",
                    "A payment record already exists; do not pay this invoice again.")
                    : passed("ERP_MATCHING", "NOT_ALREADY_PAID", "Invoice not already paid",
                    "No payment record exists in the internal ledger.", 0));
        }
    }

    private List<String> compareQr(ExtractedData data, QrPayloadData qr) {
        List<String> mismatches = new ArrayList<>();
        if (hasText(qr.sellerGstin())
                && !qr.sellerGstin().equalsIgnoreCase(data.getGstNumber())) {
            mismatches.add("seller GSTIN");
        }
        if (hasText(qr.recipientGstin()) && hasText(data.getRecipientGstin())
                && !qr.recipientGstin().equalsIgnoreCase(data.getRecipientGstin())) {
            mismatches.add("recipient GSTIN");
        }
        if (hasText(qr.invoiceNumber())
                && !qr.invoiceNumber().equalsIgnoreCase(data.getInvoiceNumber())) {
            mismatches.add("invoice number");
        }
        if (qr.invoiceDate() != null && !Objects.equals(qr.invoiceDate(), data.getInvoiceDate())) {
            mismatches.add("invoice date");
        }
        if (qr.totalAmount() != null && (data.getTotalAmount() == null
                || !nearlyEqual(qr.totalAmount(), data.getTotalAmount()))) {
            mismatches.add("total amount");
        }
        if (hasText(qr.irn())) {
            data.setIrn(qr.irn());
        }
        return mismatches;
    }

    private Integer trailingNumber(String invoiceNumber) {
        if (!hasText(invoiceNumber)) return null;
        var matcher = Pattern.compile("(\\d+)$").matcher(invoiceNumber.trim());
        if (!matcher.find()) return null;
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private VerificationOutcome outcome(List<VerificationCheck> checks) {
        int score = checks.stream().mapToInt(VerificationCheck::getScoreAwarded).sum();
        List<String> anomalies = checks.stream()
                .filter(check -> check.getStatus() == CheckStatus.FAILED)
                .map(check -> check.getDescription() + ": " + check.getDetail())
                .toList();
        return new VerificationOutcome(
                score,
                VerificationClassification.fromScore(score),
                checks,
                anomalies
        );
    }

    private VerificationCheck passed(
            String layer,
            String code,
            String description,
            String detail,
            int score
    ) {
        return new VerificationCheck(layer, code, description, CheckStatus.PASSED, detail, score);
    }

    private VerificationCheck failed(String layer, String code, String description, String detail) {
        return new VerificationCheck(layer, code, description, CheckStatus.FAILED, detail, 0);
    }

    private VerificationCheck notPerformed(
            String layer,
            String code,
            String description,
            String detail
    ) {
        return new VerificationCheck(
                layer,
                code,
                description,
                CheckStatus.NOT_PERFORMED,
                detail,
                0
        );
    }

    private boolean nearlyEqual(double left, double right) {
        return Math.abs(left - right) <= MONEY_TOLERANCE;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeGstin(String gstin) {
        return hasText(gstin) ? gstin.trim().toUpperCase(Locale.ROOT) : null;
    }

    private boolean isValidStateCode(String code) {
        int numericCode = Integer.parseInt(code);
        return (numericCode >= 1 && numericCode <= 38) || numericCode == 97;
    }

    private boolean namesMatch(String extractedName, String legalName) {
        if (!hasText(extractedName) || !hasText(legalName)) {
            return false;
        }
        String extracted = normalizeName(extractedName);
        String legal = normalizeName(legalName);
        return extracted.equals(legal) || extracted.contains(legal) || legal.contains(extracted);
    }

    private String normalizeName(String name) {
        return name.toUpperCase(Locale.ROOT)
                .replaceAll("\\b(PRIVATE|PVT|LIMITED|LTD|LLP)\\b", "")
                .replaceAll("[^A-Z0-9]", "");
    }
}
