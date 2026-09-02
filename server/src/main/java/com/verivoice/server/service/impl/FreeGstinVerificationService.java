package com.verivoice.server.service.impl;

import com.verivoice.server.entity.GstCache;
import com.verivoice.server.repository.GstCacheRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FreeGstinVerificationService {
    public record GstinVerifyResult(boolean valid, String legalName, String gstStatus, String referenceId) {
    }

    private final GstCacheRepository gstCacheRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String endpointTemplate;
    private final String apiKey;

    public FreeGstinVerificationService(
            GstCacheRepository gstCacheRepository,
            @Value("${gst.validation.endpoint:https://sheet.gstincheck.co.in/check/{apiKey}/{gstin}}") String endpointTemplate,
            @Value("${gst.validation.api-key:}") String apiKey
    ) {
        this.gstCacheRepository = gstCacheRepository;
        this.endpointTemplate = endpointTemplate;
        this.apiKey = apiKey;
    }

    @Transactional
    public Optional<GstinVerifyResult> verifyAndCache(String gstin) {
        if (apiKey == null || apiKey.isBlank() || gstin == null || gstin.isBlank()) {
            return Optional.empty();
        }

        String normalizedGstin = gstin.trim().toUpperCase();
        String url = endpointTemplate
                .replace("{apiKey}", apiKey.trim())
                .replace("{gstin}", normalizedGstin);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            boolean valid = root.path("flag").asBoolean(false)
                    || root.path("valid").asBoolean(false);
            String legalName = text(root, List.of("lgnm", "legal_name", "legalName"));
            String status = text(root, List.of("sts", "status", "gstin_status", "gstStatus"));
            String referenceId = text(root, List.of("reference_id", "referenceId", "ref_id"));

            if (valid && legalName != null && status != null) {
                gstCacheRepository.save(new GstCache(
                        normalizedGstin,
                        legalName,
                        normalizeStatus(status),
                        LocalDateTime.now()
                ));
            } else {
                gstCacheRepository.findById(normalizedGstin)
                        .ifPresent(existing -> gstCacheRepository.deleteById(existing.getGstin()));
            }
            return Optional.of(new GstinVerifyResult(valid, legalName, status, referenceId));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    private String text(JsonNode node, List<String> aliases) {
        JsonNode value = find(node, aliases);
        return value == null || value.isNull() || value.asString().isBlank() ? null : value.asString();
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
}