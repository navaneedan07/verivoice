package com.verivoice.server.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentExtractionService {

    public String extractContent(
            MultipartFile file
    ) throws Exception {

        String fileName =
                file.getOriginalFilename();

        if (fileName == null) {
            throw new RuntimeException(
                    "Invalid file"
            );
        }

        String lower =
                fileName.toLowerCase();

        if (lower.endsWith(".pdf")) {
            return extractPdf(file);
        }

        if (lower.endsWith(".docx")) {
            return extractDocx(file);
        }

        if (lower.endsWith(".txt")) {
            return new String(
                    file.getBytes()
            );
        }

        if (lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")) {

            return convertImageToBase64(file);
        }

        throw new RuntimeException(
                "Unsupported file type"
        );
    }

    private String extractPdf(
            MultipartFile file
    ) throws Exception {

        PDDocument document =
                Loader.loadPDF(
                        file.getBytes()
                );

        PDFTextStripper stripper =
                new PDFTextStripper();

        String text =
                stripper.getText(document);

        document.close();

        return text;
    }

    private String extractDocx(
            MultipartFile file
    ) throws Exception {

        XWPFDocument document =
                new XWPFDocument(
                        file.getInputStream()
                );

        XWPFWordExtractor extractor =
                new XWPFWordExtractor(
                        document
                );

        String text =
                extractor.getText();

        extractor.close();
        document.close();

        return text;
    }

    private String convertImageToBase64(
            MultipartFile file
    ) throws Exception {

        return java.util.Base64
                .getEncoder()
                .encodeToString(
                        file.getBytes()
                );
    }
}