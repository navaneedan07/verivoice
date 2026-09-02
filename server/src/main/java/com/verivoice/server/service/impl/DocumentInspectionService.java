package com.verivoice.server.service.impl;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.verivoice.server.inspection.DocumentInspection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;

@Service
public class DocumentInspectionService {
    private static final int PDF_DPI = 220;

    public DocumentInspection inspect(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        String sha256 = sha256(bytes);
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        if (name.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(bytes)) {
                String payload = decodePdfQr(document);
                return new DocumentInspection(payload != null, payload, sha256);
            }
        }
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            String payload = image == null ? null : decodeQr(image);
            return new DocumentInspection(payload != null, payload, sha256);
        }
        return DocumentInspection.unsupported(sha256);
    }

    private String decodePdfQr(PDDocument document) throws Exception {
        PDFRenderer renderer = new PDFRenderer(document);
        int pagesToScan = Math.min(document.getNumberOfPages(), 5);
        for (int page = 0; page < pagesToScan; page++) {
            String payload = decodeQr(renderer.renderImageWithDPI(page, PDF_DPI, ImageType.RGB));
            if (payload != null) return payload;
        }
        return null;
    }

    private String decodeQr(BufferedImage image) {
        String payload = decodeQrBitmap(image);
        if (payload != null) return payload;

        int width = image.getWidth() * 2;
        int height = image.getHeight() * 2;
        BufferedImage enlarged = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = enlarged.createGraphics();
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return decodeQrBitmap(enlarged);
    }

    private String decodeQrBitmap(BufferedImage image) {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
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

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
