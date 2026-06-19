package com.verivoice.server.erp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payment_records")
@Getter
@Setter
@NoArgsConstructor
public class PaymentRecord {
    @Id
    private String id = UUID.randomUUID().toString();
    private String vendorGstin;
    private String invoiceNumber;
    private BigDecimal amount;
    private LocalDate paidDate;
    private String referenceNumber;
}
