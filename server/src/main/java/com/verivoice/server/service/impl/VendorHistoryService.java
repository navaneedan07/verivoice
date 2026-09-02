package com.verivoice.server.service.impl;

import com.verivoice.server.embeddable.ExtractedData;
import com.verivoice.server.entity.GstCache;
import com.verivoice.server.entity.Vendor;
import com.verivoice.server.repository.GstCacheRepository;
import com.verivoice.server.repository.VendorRepository;
import com.verivoice.server.verification.CheckStatus;
import com.verivoice.server.verification.VerificationOutcome;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VendorHistoryService {
    private final VendorRepository vendorRepository;
    private final GstCacheRepository gstCacheRepository;

    public VendorHistoryService(
            VendorRepository vendorRepository,
            GstCacheRepository gstCacheRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.gstCacheRepository = gstCacheRepository;
    }

    public void record(ExtractedData data, VerificationOutcome outcome) {
        if (data == null || data.getGstNumber() == null || data.getGstNumber().isBlank()) {
            return;
        }
        String gstin = data.getGstNumber().trim().toUpperCase();
        Vendor vendor = vendorRepository.findById(gstin).orElseGet(Vendor::new);
        vendor.setGstin(gstin);
        vendor.setTradeName(data.getVendorName());

        GstCache cache = gstCacheRepository.findById(gstin).orElse(null);
        if (cache != null) {
            vendor.setLegalName(cache.getLegalName());
        }
        boolean gstVerified = outcome.checks().stream()
                .anyMatch(check -> "GST_STATUS".equals(check.getCode())
                        && check.getStatus() == CheckStatus.PASSED);

        if (gstVerified) {
            vendor.setVerifiedAt(LocalDateTime.now());
        }
        // Vendors should not become TRUSTED just because the invoice score is high.
        // Since this TRUSTED decision is based on our internal DB history, keep it conservative.
        vendor.setStatus("OBSERVED");

        vendorRepository.save(vendor);
    }
}
