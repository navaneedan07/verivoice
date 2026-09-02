package com.verivoice.server.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentExtractionServiceTests {
    private final DocumentExtractionService service = new DocumentExtractionService();

    @Test
    void pdfAnalysisIncludesRenderedPageImages() throws Exception {
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdfBytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                pdfBytes
        );

        DocumentExtractionService.AnalysisInput input = service.extractForAnalysis(file);

        assertThat(input.sourceContentType()).isEqualTo("application/pdf");
        assertThat(input.imageDataUrls()).hasSize(1);
        assertThat(input.imageDataUrls().get(0)).startsWith("data:image/png;base64,");
    }
}
