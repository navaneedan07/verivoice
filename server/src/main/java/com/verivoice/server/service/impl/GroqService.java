package com.verivoice.server.service.impl;

import com.verivoice.server.embeddable.ExtractedData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroqService {
    public record AnalysisResult(ExtractedData data, String rawResponse) {}

    @Value("${groq.api.key}")
    private String apiKey;
        @Value("${groq.model:meta-llama/llama-4-scout-17b-16e-instruct}")
        private String model;
    private final ObjectMapper objectMapper = new ObjectMapper();
        private final RestTemplate restTemplate = new RestTemplate();
        private final FallbackInvoiceExtractionService fallbackExtractionService;

        public GroqService(FallbackInvoiceExtractionService fallbackExtractionService) {
                this.fallbackExtractionService = fallbackExtractionService;
        }

    public AnalysisResult analyzeInvoice(String content, String contentType) {
        return analyzeInvoice(content, contentType, List.of());
    }

    public AnalysisResult analyzeInvoice(String content, String contentType, List<String> imageDataUrls) {
        ExtractedData fallback = fallbackExtractionService.extract(content);
        try {
            AnalysisResult groq = analyzeWithGroq(content, contentType, imageDataUrls);
            return new AnalysisResult(normalizeExtractedData(mergeMissing(groq.data(), fallback)), groq.rawResponse());
        } catch (RuntimeException | java.io.IOException ignored) {
            return new AnalysisResult(normalizeExtractedData(fallback), null);
        }
    }

    private AnalysisResult analyzeWithGroq(String content, String contentType, List<String> imageDataUrls) throws java.io.IOException {
        String responseJson = """
                You are an invoice parser for Indian invoices and receipts.

                Return EXACTLY ONE JSON object.
                Do not explain.
                Do not use markdown.
                Do not use ```json.
                Do not return multiple objects.

                Rules:
                - Convert all dates to yyyy-MM-dd format.
                - Use null for unknown numeric values.
                - Use an empty string for unknown text values.
                - totalAmount = final invoice amount after tax.
                - subtotal = taxable/base amount before tax.
                - taxAmount = total GST amount, preferably CGST+SGST+IGST when shown.
                - gstRate = overall GST percentage only if clearly stated or derivable.
                - Do not confuse invoice total with tax amount.
                - Prefer explicit values written on the document over guesses.
                - When images are provided, use both the images and any OCR text together.

                Return schema:
                {
                  "vendorName":"",
                  "invoiceNumber":"",
                  "purchaseOrderNumber":"",
                  "recipientGstin":"",
                  "gstNumber":"",
                  "totalAmount":null,
                  "taxAmount":null,
                  "subtotal":null,
                  "cgstAmount":null,
                  "sgstAmount":null,
                  "igstAmount":null,
                  "gstRate":null,
                  "invoiceDate":"",
                  "currency":"",
                  "paymentMethod":"",
                  "hsnSac":"",
                  "qrCode":"",
                  "irn":"",
                  "confidenceScore":0.0
                  }

                  DOCUMENT:
                  %s""".formatted(content);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        String userContent = buildUserContent(responseJson, content, contentType, imageDataUrls);
                                String body = """
                                                                {
                                                                "model":%s,
                                                                "messages":[
                                                                                                                {
                                                                                                                        "role":"system",
                                                                                                                        "content":"You are a strict invoice extraction API. Return only JSON"
                                                                                                                },
                                                                                                                {
                                                                                                                         "role":"user",
                                                                                                                         "content":%s
                                                                                                                }
                                                                                                        ],
                                                                "temperature":0,
                                                                "response_format":{
                                                                                "type":"json_object"
                                                                        }
                                                                }""".formatted(objectMapper.writeValueAsString(model), userContent);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange("https://api.groq.com/openai/v1/chat/completions",
                HttpMethod.POST,
                entity,
                String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        String message = root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asString();
        String json = extractJsonObject(message);
        return new AnalysisResult(normalizeExtractedData(objectMapper.readValue(json, ExtractedData.class)), message);

    }

    private String buildUserContent(
            String prompt,
            String content,
            String contentType,
            List<String> imageDataUrls
    ) throws java.io.IOException {
        List<String> contentParts = new ArrayList<>();
        contentParts.add("{\"type\":\"text\",\"text\":" + objectMapper.writeValueAsString(prompt) + "}");

        if (content != null && !content.isBlank() && !isImage(contentType)) {
            contentParts.add("{\"type\":\"text\",\"text\":" + objectMapper.writeValueAsString("OCR/TEXT CONTENT:\n" + content) + "}");
        }

        if (isImage(contentType) && content != null && !content.isBlank()) {
            contentParts.add("{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:" + contentType + ";base64," + content + "\"}}");
        }

        for (String imageDataUrl : imageDataUrls) {
            if (imageDataUrl != null && !imageDataUrl.isBlank()) {
                contentParts.add("{\"type\":\"image_url\",\"image_url\":{\"url\":" + objectMapper.writeValueAsString(imageDataUrl) + "}}");
            }
        }

        if (contentParts.size() == 1) {
            return objectMapper.writeValueAsString(prompt);
        }
        return "[" + String.join(",", contentParts) + "]";
    }

        private boolean isImage(String contentType) {
                return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/");
        }

        private ExtractedData mergeMissing(ExtractedData primary, ExtractedData fallback) {
                if (primary == null) return fallback;
                if (blank(primary.getVendorName())) primary.setVendorName(fallback.getVendorName());
                if (blank(primary.getInvoiceNumber())) primary.setInvoiceNumber(fallback.getInvoiceNumber());
                if (blank(primary.getGstNumber())) primary.setGstNumber(fallback.getGstNumber());
                if (primary.getInvoiceDate() == null) primary.setInvoiceDate(fallback.getInvoiceDate());
                if (emptyNumber(primary.getSubtotal()) && fallback.getSubtotal() != null) primary.setSubtotal(fallback.getSubtotal());
                if (emptyNumber(primary.getTaxAmount()) && fallback.getTaxAmount() != null) primary.setTaxAmount(fallback.getTaxAmount());
                if (emptyNumber(primary.getTotalAmount()) && fallback.getTotalAmount() != null) primary.setTotalAmount(fallback.getTotalAmount());
                if (emptyNumber(primary.getCgstAmount()) && fallback.getCgstAmount() != null) primary.setCgstAmount(fallback.getCgstAmount());
                if (emptyNumber(primary.getSgstAmount()) && fallback.getSgstAmount() != null) primary.setSgstAmount(fallback.getSgstAmount());
                if (emptyNumber(primary.getIgstAmount()) && fallback.getIgstAmount() != null) primary.setIgstAmount(fallback.getIgstAmount());
                if (emptyNumber(primary.getGstRate()) && fallback.getGstRate() != null) primary.setGstRate(fallback.getGstRate());
                if (blank(primary.getCurrency())) primary.setCurrency(fallback.getCurrency());
                if (blank(primary.getHsnSac())) primary.setHsnSac(fallback.getHsnSac());
                if (primary.getConfidenceScore() == null || primary.getConfidenceScore() <= 0) primary.setConfidenceScore(fallback.getConfidenceScore());
                return primary;
        }

        ExtractedData normalizeExtractedData(ExtractedData data) {
                if (data == null) return null;
                if (!blank(data.getGstNumber())) data.setGstNumber(data.getGstNumber().trim().toUpperCase(java.util.Locale.ROOT));
                if (!blank(data.getRecipientGstin())) data.setRecipientGstin(data.getRecipientGstin().trim().toUpperCase(java.util.Locale.ROOT));
                if (blank(data.getCurrency())) data.setCurrency("INR");

                if (shouldIgnoreIgstNoise(data)) {
                        data.setIgstAmount(0d);
                }

                Double componentsTax = sumPresent(data.getCgstAmount(), data.getSgstAmount(), data.getIgstAmount());
                if (emptyNumber(data.getTaxAmount()) && componentsTax != null) data.setTaxAmount(componentsTax);

                if (componentsTax != null && positive(data.getTaxAmount())) {
                        boolean componentsCloserToTotal = positive(data.getSubtotal())
                                        && positive(data.getTotalAmount())
                                        && distance(data.getSubtotal() + componentsTax, data.getTotalAmount())
                                        < distance(data.getSubtotal() + data.getTaxAmount(), data.getTotalAmount());
                        if (componentsCloserToTotal || nearlyEqual(componentsTax, data.getTaxAmount(), 2.0d)) {
                                data.setTaxAmount(componentsTax);
                        }
                }

                if (emptyNumber(data.getSubtotal()) && positive(data.getTotalAmount()) && positive(data.getTaxAmount())) {
                        double derivedSubtotal = data.getTotalAmount() - data.getTaxAmount();
                        if (derivedSubtotal >= 0d) data.setSubtotal(derivedSubtotal);
                }

                if (positive(data.getSubtotal()) && positive(data.getTaxAmount())
                                && (!positive(data.getTotalAmount()) || data.getTotalAmount() <= data.getSubtotal())) {
                        data.setTotalAmount(data.getSubtotal() + data.getTaxAmount());
                }

                if (positive(data.getSubtotal()) && positive(data.getTaxAmount()) && positive(data.getTotalAmount())
                                && data.getSubtotal() + data.getTaxAmount() > data.getTotalAmount() + 1.0d) {
                        data.setTotalAmount(data.getSubtotal() + data.getTaxAmount());
                }

                if (emptyNumber(data.getTotalAmount()) && positive(data.getSubtotal()) && positive(data.getTaxAmount())) {
                        data.setTotalAmount(data.getSubtotal() + data.getTaxAmount());
                }

                if (emptyNumber(data.getGstRate()) && positive(data.getSubtotal()) && positive(data.getTaxAmount())) {
                        data.setGstRate((data.getTaxAmount() / data.getSubtotal()) * 100d);
                }
                return data;
        }

        private boolean blank(String value) {
                return value == null || value.isBlank();
        }

        private boolean emptyNumber(Double value) {
                return value == null || value <= 0d;
        }

        private boolean positive(Double value) {
                return value != null && value > 0d;
        }

        private Double sumPresent(Double... values) {
                double total = 0d;
                boolean found = false;
                for (Double value : values) {
                        if (positive(value)) {
                                total += value;
                                found = true;
                        }
                }
                return found ? total : null;
        }

        private boolean shouldIgnoreIgstNoise(ExtractedData data) {
                if (!positive(data.getCgstAmount()) || !positive(data.getSgstAmount()) || !positive(data.getIgstAmount())) {
                        return false;
                }
                double pairTax = data.getCgstAmount() + data.getSgstAmount();
                if (data.getIgstAmount() <= 1.0d) {
                        return true;
                }
                if (!positive(data.getSubtotal()) || !positive(data.getTotalAmount())) {
                        return false;
                }
                double withPair = distance(data.getSubtotal() + pairTax, data.getTotalAmount());
                double withIgst = distance(data.getSubtotal() + pairTax + data.getIgstAmount(), data.getTotalAmount());
                return withPair + 0.01d < withIgst;
        }

        private double distance(double left, double right) {
                return Math.abs(left - right);
        }

        private boolean nearlyEqual(double left, double right, double tolerance) {
                return Math.abs(left - right) <= tolerance;
        }

        private String extractJsonObject(String response) {
                int start = response == null ? -1 : response.indexOf('{');
                int end = response == null ? -1 : response.lastIndexOf('}');
                if (start < 0 || end < start) {
                        throw new IllegalArgumentException("Groq response did not contain a JSON object");
                }
                return response.substring(start, end + 1);
        }
}
