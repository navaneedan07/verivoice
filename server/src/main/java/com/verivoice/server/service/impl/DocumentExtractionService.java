package com.verivoice.server.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class DocumentExtractionService {
    private static final int PDF_PREVIEW_DPI = 160;
    private static final int MAX_PDF_PAGES_FOR_VISION = 2;

    public record AnalysisInput(
            String extractedText,
            List<String> imageDataUrls,
            String sourceContentType
    ) {
    }

    public AnalysisInput extractForAnalysis(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new RuntimeException("Invalid file");
        }

        String lower = fileName.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".pdf")) {
            return extractPdfForAnalysis(file);
        }

        if (lower.endsWith(".docx")) {
            return new AnalysisInput(extractDocx(file), List.of(), file.getContentType());
        }

        if (lower.endsWith(".txt")) {
            return new AnalysisInput(new String(file.getBytes(), StandardCharsets.UTF_8), List.of(), file.getContentType());
        }

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
            return new AnalysisInput("", List.of(toDataUrl(file.getBytes(), file.getContentType())), file.getContentType());
        }

        throw new RuntimeException("Unsupported file type");
    }

    public String extractContent(MultipartFile file) throws Exception {
        return extractForAnalysis(file).extractedText();
    }

    private AnalysisInput extractPdfForAnalysis(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(document);
            List<String> imageDataUrls = renderPdfPages(document);
            return new AnalysisInput(text, imageDataUrls, "application/pdf");
        }
    }

    private String extractDocx(MultipartFile file) throws Exception {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private List<String> renderPdfPages(PDDocument document) throws Exception {
        PDFRenderer renderer = new PDFRenderer(document);
        int pages = Math.min(document.getNumberOfPages(), MAX_PDF_PAGES_FOR_VISION);
        List<String> images = new ArrayList<>(pages);
        for (int page = 0; page < pages; page++) {
            BufferedImage image = renderer.renderImageWithDPI(page, PDF_PREVIEW_DPI, ImageType.RGB);
            images.add(toPngDataUrl(image));
        }
        return images;
    }

    private String toDataUrl(byte[] bytes, String contentType) {
        String mimeType = contentType == null || contentType.isBlank() ? "image/png" : contentType;
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String toPngDataUrl(BufferedImage image) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }
}
