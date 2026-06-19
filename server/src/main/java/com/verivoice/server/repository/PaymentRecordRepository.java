package com.verivoice.server.repository;

import com.verivoice.server.erp.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, String> {
    boolean existsByVendorGstinIgnoreCaseAndInvoiceNumberIgnoreCase(
            String vendorGstin,
            String invoiceNumber
    );
}
