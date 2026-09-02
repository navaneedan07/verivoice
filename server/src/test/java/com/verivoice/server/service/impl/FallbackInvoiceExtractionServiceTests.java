package com.verivoice.server.service.impl;

import com.verivoice.server.embeddable.ExtractedData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackInvoiceExtractionServiceTests {
    private final FallbackInvoiceExtractionService service = new FallbackInvoiceExtractionService();

    @Test
    void extractsReceiptHeadingAndAggregatesTaxRows() {
        String receipt = """
                WONDERLA HOLIDAYS LIMITED
                GSTIN: 33AAACW4514C1ZW
                Sales Order ID: SO-202606112340471815
                Sales Order Time: 11/06/2026 23:40:47
                Subtotal ₹ 919.00
                CGST 9% ₹ 82.71
                SGST 9% ₹ 82.71
                CGST 9% ₹ 4.50
                SGST 9% ₹ 4.50
                Total ₹ 1,143.42
                """;

        ExtractedData data = service.extract(receipt);

        assertThat(data.getVendorName()).isEqualTo("WONDERLA HOLIDAYS LIMITED");
        assertThat(data.getGstNumber()).isEqualTo("33AAACW4514C1ZW");
        assertThat(data.getInvoiceNumber()).isEqualTo("SO-202606112340471815");
        assertThat(data.getInvoiceDate()).isEqualTo(LocalDate.of(2026, 6, 11));
        assertThat(data.getSubtotal()).isEqualTo(919.00);
        assertThat(data.getCgstAmount()).isEqualTo(87.21);
        assertThat(data.getSgstAmount()).isEqualTo(87.21);
        assertThat(data.getTaxAmount()).isEqualTo(174.42);
        assertThat(data.getTotalAmount()).isEqualTo(1143.42);
    }

    @Test
    void derivesSubtotalAndGstRateWhenOnlyTotalAndTaxAreExplicit() {
        String receipt = """
                Apex Office Systems Pvt. Ltd.
                GSTIN: 29AAIZP2912R1ZR
                Tax Invoice No: INV-2026-003A
                Invoice Date: 11/07/2028
                Total Tax: 13,000.00
                Grand Total: 1,09,500.00
                """;

        ExtractedData data = service.extract(receipt);

        assertThat(data.getVendorName()).isEqualTo("Apex Office Systems Pvt. Ltd.");
        assertThat(data.getInvoiceNumber()).isEqualTo("INV-2026-003A");
        assertThat(data.getGstNumber()).isEqualTo("29AAIZP2912R1ZR");
        assertThat(data.getInvoiceDate()).isEqualTo(LocalDate.of(2028, 7, 11));
        assertThat(data.getTaxAmount()).isEqualTo(13000.00);
        assertThat(data.getTotalAmount()).isEqualTo(109500.00);
        assertThat(data.getSubtotal()).isEqualTo(96500.00);
        assertThat(data.getGstRate()).isCloseTo(13.4715, org.assertj.core.data.Offset.offset(0.001));
    }
}
