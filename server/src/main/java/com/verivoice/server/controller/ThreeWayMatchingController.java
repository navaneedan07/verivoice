package com.verivoice.server.controller;

import com.verivoice.server.erp.GoodsReceipt;
import com.verivoice.server.erp.PurchaseOrder;
import com.verivoice.server.repository.GoodsReceiptRepository;
import com.verivoice.server.repository.PurchaseOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/erp/matching")
public class ThreeWayMatchingController {
    private final PurchaseOrderRepository poRepository;
    private final GoodsReceiptRepository grRepository;

    public ThreeWayMatchingController(
            PurchaseOrderRepository poRepository,
            GoodsReceiptRepository grRepository
    ) {
        this.poRepository = poRepository;
        this.grRepository = grRepository;
    }

    /**
     * 3-Way Matching: Purchase Order + Goods Receipt + Invoice
     * Validates that all three documents match before payment authorization
     */
    @PostMapping("/validate")
    public ResponseEntity<ThreeWayMatchResult> validateThreeWayMatch(
            @RequestParam String poNumber,
            @RequestParam String grnNumber,
            @RequestBody ThreeWayMatchRequest invoiceData
    ) {
        var result = new ThreeWayMatchResult();

        // Fetch PO and GR
        PurchaseOrder po = poRepository.findByPoNumberIgnoreCase(poNumber).orElse(null);
        GoodsReceipt gr = grRepository.findByGrnNumberIgnoreCase(grnNumber).orElse(null);

        // Check 1: PO exists
        if (po == null) {
            result.setPoMatch(false);
            result.addIssue("Purchase Order not found");
        } else {
            result.setPoMatch(true);
        }

        // Check 2: GR exists
        if (gr == null) {
            result.setGrMatch(false);
            result.addIssue("Goods Receipt not found");
        } else {
            result.setGrMatch(true);
        }

        // Check 3: PO and GR linked correctly
        if (po != null && gr != null) {
            boolean poGrMatch = po.getPoNumber().equalsIgnoreCase(gr.getPoNumber());
            result.setPoGrLinkMatch(poGrMatch);
            if (!poGrMatch) {
                result.addIssue("PO and GR not linked: GR references PO " + gr.getPoNumber() + " but checking PO " + po.getPoNumber());
            }
        }

        // Check 4: Invoice amount matches PO amount
        if (po != null) {
            boolean invoiceAmountMatch = Math.abs(invoiceData.invoiceAmount - po.getAmount().doubleValue()) < 0.01;
            result.setAmountMatch(invoiceAmountMatch);
            if (!invoiceAmountMatch) {
                result.addIssue("Invoice amount (" + invoiceData.invoiceAmount + ") ≠ PO amount (" + po.getAmount() + ")");
            }
        }

        // Check 5: GSTIN matching
        if (po != null && !po.getVendorGstin().equalsIgnoreCase(invoiceData.vendorGstin)) {
            result.addIssue("Vendor GSTIN mismatch: PO has " + po.getVendorGstin() + " but invoice has " + invoiceData.vendorGstin);
            result.setGstinMatch(false);
        } else {
            result.setGstinMatch(true);
        }

        // Overall status
        result.setAllMatch(result.isPoMatch() && result.isGrMatch() && result.isPoGrLinkMatch() && result.isAmountMatch() && result.isGstinMatch());

        return ResponseEntity.ok(result);
    }

    public static class ThreeWayMatchRequest {
        public String invoiceNumber;
        public Double invoiceAmount;
        public String vendorGstin;
        public String invoiceDate;

        public ThreeWayMatchRequest() {}

        public ThreeWayMatchRequest(String invoiceNumber, Double invoiceAmount, String vendorGstin, String invoiceDate) {
            this.invoiceNumber = invoiceNumber;
            this.invoiceAmount = invoiceAmount;
            this.vendorGstin = vendorGstin;
            this.invoiceDate = invoiceDate;
        }
    }

    public static class ThreeWayMatchResult {
        private boolean poMatch;
        private boolean grMatch;
        private boolean poGrLinkMatch;
        private boolean amountMatch;
        private boolean gstinMatch;
        private boolean allMatch;
        private Map<String, String> issues = new HashMap<>();

        public void addIssue(String issue) {
            issues.put("issue_" + issues.size(), issue);
        }

        // Getters and setters
        public boolean isPoMatch() { return poMatch; }
        public void setPoMatch(boolean poMatch) { this.poMatch = poMatch; }

        public boolean isGrMatch() { return grMatch; }
        public void setGrMatch(boolean grMatch) { this.grMatch = grMatch; }

        public boolean isPoGrLinkMatch() { return poGrLinkMatch; }
        public void setPoGrLinkMatch(boolean poGrLinkMatch) { this.poGrLinkMatch = poGrLinkMatch; }

        public boolean isAmountMatch() { return amountMatch; }
        public void setAmountMatch(boolean amountMatch) { this.amountMatch = amountMatch; }

        public boolean isGstinMatch() { return gstinMatch; }
        public void setGstinMatch(boolean gstinMatch) { this.gstinMatch = gstinMatch; }

        public boolean isAllMatch() { return allMatch; }
        public void setAllMatch(boolean allMatch) { this.allMatch = allMatch; }

        public Map<String, String> getIssues() { return issues; }
        public void setIssues(Map<String, String> issues) { this.issues = issues; }
    }
}
