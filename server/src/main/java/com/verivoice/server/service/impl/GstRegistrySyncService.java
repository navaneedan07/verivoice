package com.verivoice.server.service.impl;

import com.verivoice.server.entity.GstCache;
import com.verivoice.server.repository.GstCacheRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class GstRegistrySyncService {
    private final GstCacheRepository cacheRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String lookupUrl;
    private final String bearerToken;
    private final String clientId;
    private final String clientSecret;
    private final String appyflowUrl;
    private final String appyflowKeySecret;

    public GstRegistrySyncService(
            GstCacheRepository cacheRepository,
            @Value("${gst.registry.lookup-url:}") String lookupUrl,
            @Value("${gst.registry.bearer-token:}") String bearerToken,
            @Value("${gst.registry.client-id:}") String clientId,
            @Value("${gst.registry.client-secret:}") String clientSecret,
            @Value("${appyflow.gst.url:https://appyflow.in/api/verifyGST}") String appyflowUrl,
            @Value("${appyflow.key-secret:}") String appyflowKeySecret
    ) {
        this.cacheRepository = cacheRepository;
        this.lookupUrl = lookupUrl;
        this.bearerToken = bearerToken;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.appyflowUrl = appyflowUrl;
        this.appyflowKeySecret = appyflowKeySecret;
    }

    public void refreshIfConfigured(String gstin) {
        if (gstin == null || gstin.isBlank()) {
            return;
        }
        if (appyflowKeySecret != null && !appyflowKeySecret.isBlank()) {
            refreshFromAppyflow(gstin);
            return;
        }
        refreshFromGenericProvider(gstin);
    }

    private void refreshFromAppyflow(String gstin) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String url = UriComponentsBuilder.fromUriString(appyflowUrl)
                    .queryParam("gstNo", gstin)
                    .queryParam("key_secret", appyflowKeySecret)
                    .build()
                    .encode()
                    .toUriString();
            fetchAndCache(gstin, url, headers);
        } catch (RuntimeException ignored) {
            // Provider failure leaves the GST check explicitly unavailable.
        }
    }

    private void refreshFromGenericProvider(String gstin) {
        if (lookupUrl == null || lookupUrl.isBlank()) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            if (bearerToken != null && !bearerToken.isBlank()) {
                headers.setBearerAuth(bearerToken);
            }
            if (clientId != null && !clientId.isBlank()) {
                headers.set("client-id", clientId);
            }
            if (clientSecret != null && !clientSecret.isBlank()) {
                headers.set("client-secret", clientSecret);
            }
            String url = lookupUrl.replace("{gstin}", gstin);
            fetchAndCache(gstin, url, headers);
        } catch (RuntimeException ignored) {
            // Registry availability must not destroy the invoice audit; the check remains NOT_PERFORMED.
        }
    }

    private void fetchAndCache(String gstin, String url, HttpHeaders headers) {
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        JsonNode root = objectMapper.readTree(response.getBody());
        if (root.path("error").asBoolean(false)) {
            return;
        }
        String legalName = text(root, List.of("legalName", "LegalName", "lgnm"));
        String status = text(root, List.of("status", "Status", "sts", "gstinStatus"));
        if (legalName != null && status != null) {
            cacheRepository.save(new GstCache(
                    gstin,
                    legalName,
                    normalizeStatus(status),
                    LocalDateTime.now()
            ));
        }
    }

    private String normalizeStatus(String status) {
        return "ACTIVE".equalsIgnoreCase(status) ? "ACTIVE" : status.toUpperCase();
    }

    private String text(JsonNode node, List<String> aliases) {
        JsonNode value = find(node, aliases);
        return value == null || value.isNull() ? null : value.asText();
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
