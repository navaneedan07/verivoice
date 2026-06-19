package com.verivoice.server.inspection;

import java.time.LocalDate;

public record QrPayloadData(
        String sellerGstin,
        String recipientGstin,
        String invoiceNumber,
        LocalDate invoiceDate,
        Double totalAmount,
        String irn,
        boolean structured
) {
}
