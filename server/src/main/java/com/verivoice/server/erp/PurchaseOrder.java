package com.verivoice.server.erp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder {
    @Id
    private String id = UUID.randomUUID().toString();
    private String poNumber;
    private String vendorGstin;
    private String vendorName;
    private BigDecimal amount;
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus status = PurchaseOrderStatus.OPEN;

    public enum PurchaseOrderStatus {
        OPEN, PARTIALLY_RECEIVED, RECEIVED, CLOSED, CANCELLED
    }
}
