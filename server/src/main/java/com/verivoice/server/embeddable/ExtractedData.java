package com.verivoice.server.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class ExtractedData {
    @Column(length = 200)
    private String vendorName;
    @Column(length = 100)
    private String invoiceNumber;
    @Column(length = 100)
    private String purchaseOrderNumber;
    @Column(length = 15)
    private String recipientGstin;
    @Column(length = 15)
    private String gstNumber;
    private Double totalAmount;
    private Double taxAmount;
    private Double subtotal;
    private Double cgstAmount;
    private Double sgstAmount;
    private Double igstAmount;
    private Double gstRate;
    private LocalDate invoiceDate;
    @Column(length = 10)
    private String currency;
    @Column(length = 50)
    private String paymentMethod;
    @Column(length = 100)
    private String hsnSac;
    @Column(length = 4000)
    private String qrCode;
    @Column(length = 100)
    private String irn;
    private Double confidenceScore;
}
