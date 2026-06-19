package com.verivoice.server.repository;

import com.verivoice.server.erp.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, String> {
    boolean existsByPoNumberIgnoreCaseAndAcceptedTrue(String poNumber);
    Optional<GoodsReceipt> findByGrnNumberIgnoreCase(String grnNumber);
}
