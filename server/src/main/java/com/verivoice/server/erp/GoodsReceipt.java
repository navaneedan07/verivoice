package com.verivoice.server.erp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@NoArgsConstructor
public class GoodsReceipt {
    @Id
    private String id = UUID.randomUUID().toString();
    private String grnNumber;
    private String poNumber;
    private LocalDate receivedDate;
    private Boolean accepted = true;
}
