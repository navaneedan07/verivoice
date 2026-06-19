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
        String candidate = unwrapJwtPayload(payload.trim());
        try {
            JsonNode root = objectMapper.readTree(candidate);
            return new QrPayloadData(
                    text(root, List.of("SellerGstin", "SellerGSTIN", "supplierGstin", "gstin")),
                    text(root, List.of("BuyerGstin", "BuyerGSTIN", "recipientGstin")),
                    text(root, List.of("DocNo", "invoiceNumber", "InvNo")),
                    date(text(root, List.of("DocDt", "invoiceDate", "InvDt"))),
                    number(root, List.of("TotInvVal", "totalAmount", "invoiceValue")),
                    text(root, List.of("Irn", "IRN", "irn")),
                    true
            );
        } catch (RuntimeException ignored) {
            return new QrPayloadData(null, null, null, null, null, null, false);
        }
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
        return found == null || found.isNull() ? null : found.asText();
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
}
