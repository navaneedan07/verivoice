package com.verivoice.server.service.impl;

import com.verivoice.server.inspection.QrPayloadData;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class QrPayloadService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QrPayloadData parse(String payload) {
        if (payload == null || payload.isBlank()) {
            return new QrPayloadData(null, null, null, null, null, null, false);
        }

        String raw = payload.trim();
        String candidate = unwrapJwtPayload(raw);

        // Fallback IRN extraction from raw/decoded QR payload even when JSON parsing fails.
        // IRN in IRP e-invoice QR is typically a 64-hex string (base16) or similar token.
        String irnFallback = extractIrnFallback(raw);

        try {
            JsonNode root = objectMapper.readTree(candidate);
            String irn = text(root, List.of("Irn", "IRN", "irn"));
            if (!hasText(irn)) {
                irn = irnFallback;
            }
            return new QrPayloadData(
                    text(root, List.of("SellerGstin", "SellerGSTIN", "supplierGstin", "gstin")),
                    text(root, List.of("BuyerGstin", "BuyerGSTIN", "recipientGstin")),
                    text(root, List.of("DocNo", "invoiceNumber", "InvNo")),
                    date(text(root, List.of("DocDt", "invoiceDate", "InvDt"))),
                    number(root, List.of("TotInvVal", "totalAmount", "invoiceValue")),
                    irn,
                    true
            );
        } catch (RuntimeException ignored) {
            QrPayloadData loose = parseLooseText(raw, irnFallback);
            return new QrPayloadData(
                    loose.sellerGstin(),
                    loose.recipientGstin(),
                    loose.invoiceNumber(),
                    loose.invoiceDate(),
                    loose.totalAmount(),
                    loose.irn(),
                    loose.sellerGstin() != null
                            || loose.invoiceNumber() != null
                            || loose.invoiceDate() != null
                            || loose.totalAmount() != null
            );
        }
    }

    private String extractIrnFallback(String text) {
        if (text == null) return null;
        // Most common pattern: 64 hex chars
        var matcher = java.util.regex.Pattern.compile("\\b[0-9a-fA-F]{64}\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private String unwrapJwtPayload(String payload) {
        String[] parts = payload.split("\\.");
        if (parts.length == 3) {
            try {
                return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                return payload;
            }
        }
        return payload;
    }

    private String text(JsonNode node, List<String> aliases) {
        JsonNode found = find(node, aliases);
        return found == null || found.isNull() ? null : found.asString();
    }

    private Double number(JsonNode node, List<String> aliases) {
        JsonNode found = find(node, aliases);
        if (found == null || found.isNull()) return null;
        try {
            return found.asDouble();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private JsonNode find(JsonNode node, List<String> aliases) {
        if (node == null) return null;
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(field.getKey()))) {
                    return field.getValue();
                }
                JsonNode nested = find(field.getValue(), aliases);
                if (nested != null) return nested;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode nested = find(child, aliases);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private LocalDate date(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
                DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT)
        )) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private QrPayloadData parseLooseText(String payload, String irn) {
        String sellerGstin = firstMatch(payload, "\\b[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]\\b");
        String invoiceNumber = firstGroup(payload, "(?i)(?:doc(?:ument)?\\s*no|docno|invoice\\s*no|inv(?:oice)?\\s*number)\\s*[:=\\-]?\\s*([A-Z0-9./_-]+)");
        LocalDate invoiceDate = date(firstGroup(payload, "(?i)(?:doc(?:ument)?\\s*dt|docdt|invoice\\s*date|inv(?:oice)?\\s*dt)\\s*[:=\\-]?\\s*([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})"));
        Double totalAmount = parseNumber(firstGroup(payload, "(?i)(?:tot(?:al)?\\s*inv(?:oice)?\\s*val(?:ue)?|total\\s*amount|invoice\\s*value)\\s*[:=\\-]?\\s*([0-9,]+(?:\\.[0-9]+)?)"));
        return new QrPayloadData(sellerGstin, null, invoiceNumber, invoiceDate, totalAmount, irn, false);
    }

    private String firstGroup(String source, String expression) {
        var matcher = java.util.regex.Pattern.compile(expression).matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String firstMatch(String source, String expression) {
        var matcher = java.util.regex.Pattern.compile(expression).matcher(source);
        return matcher.find() ? matcher.group() : null;
    }

    private Double parseNumber(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
