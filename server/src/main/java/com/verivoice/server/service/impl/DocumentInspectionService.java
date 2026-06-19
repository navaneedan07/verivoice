package com.verivoice.server.service.impl;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.verivoice.server.inspection.DocumentInspection;
import com.verivoice.server.embeddable.ExtractedData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.io.IOException;

@Service
public class DocumentInspectionService {
    private static final int PDF_DPI = 220;

    public DocumentInspection inspect(MultipartFile file, ExtractedData extractedData) throws Exception {
        String name = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase();
        byte[] bytes = file.getBytes();
        String sha256 = sha256(bytes);

        if (name.endsWith(".pdf")) {
            return inspectPdf(bytes, sha256, extractedData == null ? null : extractedData.getTotalAmount());
        }
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            String payload = image == null ? null : decodeQr(image);
            return new DocumentInspection(
                    false, payload != null, payload, false, false, false,
                    0, false, false, false, false,
                    payload == null ? List.of("No QR code was decoded from the image.")
                            : List.of("QR code decoded from the uploaded image."),
                    sha256
            );
        }
        return DocumentInspection.unsupported(sha256);
    }

    private DocumentInspection inspectPdf(byte[] bytes, String sha256, Double totalAmount) throws Exception {
        List<String> notes = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String qrPayload = decodePdfQr(document);
            Set<String> fonts = collectFonts(document);
            String text = new PDFTextStripper().getText(document);
            boolean imageOnly = text == null || text.isBlank();
            boolean signed = !document.getSignatureDictionaries().isEmpty();
            boolean editedMetadata = metadataSuggestsEditing(document);
            boolean multipleRevisions = countOccurrences(bytes, "%%EOF") > 1;
            boolean suspiciousFonts = fonts.size() > 8;
            TypographyResult typography = inspectTypography(document, totalAmount);

            if (qrPayload != null) notes.add("QR code decoded from rendered PDF pixels.");
            else notes.add("No decodable QR code found in rendered PDF pages.");
            if (signed) notes.add("PDF contains at least one embedded digital signature.");
            if (editedMetadata) notes.add("PDF modification timestamp is later than creation timestamp.");
            if (multipleRevisions) notes.add("PDF contains multiple end-of-file markers, suggesting incremental revisions.");
            if (suspiciousFonts) notes.add("Unusually high font count: " + fonts.size() + ".");
            if (typography.anomaly()) {
                notes.add("Invoice total uses typography inconsistent with the document's dominant font.");
            }
            if (imageOnly) notes.add("PDF has no extractable text and appears image-only.");

            return new DocumentInspection(
                    true,
                    qrPayload != null,
                    qrPayload,
                    signed,
                    editedMetadata,
                    multipleRevisions,
                    fonts.size(),
                    suspiciousFonts,
                    typography.checked(),
                    typography.anomaly(),
                    imageOnly,
                    notes,
                    sha256
            );
        }
    }

    private String decodePdfQr(PDDocument document) throws Exception {
        PDFRenderer renderer = new PDFRenderer(document);
        int pagesToScan = Math.min(document.getNumberOfPages(), 5);
        for (int page = 0; page < pagesToScan; page++) {
            BufferedImage image = renderer.renderImageWithDPI(page, PDF_DPI, ImageType.RGB);
            String payload = decodeQr(image);
            if (payload != null) {
                return payload;
            }
        }
        return null;
    }

    private String decodeQr(BufferedImage image) {
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image))
        );
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);
        try {
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException ignored) {
            return null;
        }
    }

    private Set<String> collectFonts(PDDocument document) throws Exception {
        Set<String> fonts = new HashSet<>();
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) continue;
            for (COSName name : resources.getFontNames()) {
                PDFont font = resources.getFont(name);
                if (font != null) {
                    fonts.add(font.getName());
                }
            }
        }
        return fonts;
    }

    private TypographyResult inspectTypography(PDDocument document, Double totalAmount)
            throws IOException {
        if (totalAmount == null) {
            return new TypographyResult(false, false);
        }
        TypographyStripper stripper = new TypographyStripper(totalAmount);
        stripper.getText(document);
        return stripper.result();
    }

    private boolean metadataSuggestsEditing(PDDocument document) {
        var info = document.getDocumentInformation();
        return info.getCreationDate() != null
                && info.getModificationDate() != null
                && info.getModificationDate().after(info.getCreationDate());
    }

    private int countOccurrences(byte[] bytes, String marker) {
        String content = new String(bytes, StandardCharsets.ISO_8859_1);
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(marker, index)) >= 0) {
            count++;
            index += marker.length();
        }
        return count;
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record TypographyResult(boolean checked, boolean anomaly) {
    }

    private static class TypographyStripper extends PDFTextStripper {
        private final Set<String> amountForms;
        private final Map<String, Integer> allFontCounts = new HashMap<>();
        private final Map<String, Integer> amountFontCounts = new HashMap<>();
        private boolean amountSeen;

        TypographyStripper(double totalAmount) throws IOException {
            String plain = String.format(java.util.Locale.ROOT, "%.2f", totalAmount);
            String grouped = String.format(java.util.Locale.US, "%,.2f", totalAmount);
            // Use LinkedHashSet to avoid duplicates while maintaining order
            Set<String> forms = new java.util.LinkedHashSet<>();
            forms.add(plain);
            forms.add(grouped);
            forms.add(plain.replace(".00", ""));
            forms.add(grouped.replace(".00", ""));
            this.amountForms = Set.copyOf(forms);
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions) {
            for (TextPosition position : positions) {
                allFontCounts.merge(position.getFont().getName(), 1, Integer::sum);
            }
            String normalized = text.replace("₹", "").replace("INR", "").trim();
            boolean containsAmount = amountForms.stream().anyMatch(normalized::contains);
            if (containsAmount) {
                amountSeen = true;
                for (TextPosition position : positions) {
                    amountFontCounts.merge(position.getFont().getName(), 1, Integer::sum);
                }
            }
        }

        TypographyResult result() {
            if (!amountSeen || allFontCounts.isEmpty() || amountFontCounts.isEmpty()) {
                return new TypographyResult(false, false);
            }
            String dominantFont = allFontCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
            String amountFont = amountFontCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
            int amountFontUse = allFontCounts.getOrDefault(amountFont, 0);
            boolean rareAmountFont = amountFontUse < Math.max(3, allFontCounts.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum() / 100);
            return new TypographyResult(true, !amountFont.equals(dominantFont) && rareAmountFont);
        }
    }
}
