package com.verivoice.server.controller;

import com.verivoice.server.erp.GoodsReceipt;
import com.verivoice.server.erp.PaymentRecord;
import com.verivoice.server.erp.PurchaseOrder;
import com.verivoice.server.repository.GoodsReceiptRepository;
import com.verivoice.server.repository.PaymentRecordRepository;
import com.verivoice.server.repository.PurchaseOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/erp")
public class ErpController {
    private final PurchaseOrderRepository purchaseOrders;
    private final GoodsReceiptRepository goodsReceipts;
    private final PaymentRecordRepository payments;

    public ErpController(
            PurchaseOrderRepository purchaseOrders,
            GoodsReceiptRepository goodsReceipts,
            PaymentRecordRepository payments
    ) {
        this.purchaseOrders = purchaseOrders;
        this.goodsReceipts = goodsReceipts;
        this.payments = payments;
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(@RequestBody PurchaseOrder order) {
        PurchaseOrder saved = purchaseOrders.save(order);
        return ResponseEntity.created(URI.create("/api/erp/purchase-orders/" + saved.getId()))
                .body(saved);
    }

    @GetMapping("/purchase-orders")
    public List<PurchaseOrder> listPurchaseOrders() {
        return purchaseOrders.findAll();
    }

    @PostMapping("/goods-receipts")
    public ResponseEntity<GoodsReceipt> createGoodsReceipt(@RequestBody GoodsReceipt receipt) {
        GoodsReceipt saved = goodsReceipts.save(receipt);
        return ResponseEntity.created(URI.create("/api/erp/goods-receipts/" + saved.getId()))
                .body(saved);
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentRecord> createPayment(@RequestBody PaymentRecord payment) {
        PaymentRecord saved = payments.save(payment);
        return ResponseEntity.created(URI.create("/api/erp/payments/" + saved.getId()))
                .body(saved);
    }
}
