package com.verivoice.server.service.impl;

import com.verivoice.server.inspection.QrPayloadData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class QrPayloadServiceTests {
    private final QrPayloadService service = new QrPayloadService();

    @Test
    void parsesLooseTextQrPayloadWhenJsonParsingFails() {
        String payload = """
                SellerGstin=29AAIZP2912R1ZR
                DocNo=INV-2026-003A
                DocDt=11/07/2028
                TotInvVal=109500.00
                """;

        QrPayloadData data = service.parse(payload);

        assertThat(data.structured()).isTrue();
        assertThat(data.sellerGstin()).isEqualTo("29AAIZP2912R1ZR");
        assertThat(data.invoiceNumber()).isEqualTo("INV-2026-003A");
        assertThat(data.invoiceDate()).isEqualTo(LocalDate.of(2028, 7, 11));
        assertThat(data.totalAmount()).isEqualTo(109500.00);
    }
}
