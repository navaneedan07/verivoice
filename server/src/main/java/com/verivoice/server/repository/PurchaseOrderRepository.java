package com.verivoice.server.repository;

import com.verivoice.server.erp.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
    Optional<PurchaseOrder> findByPoNumberIgnoreCase(String poNumber);
}
