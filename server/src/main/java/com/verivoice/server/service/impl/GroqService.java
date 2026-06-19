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

@Service
public class GroqService {
    @Value("${groq.api.key}")
    private String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public ExtractedData analyzeInvoice(String content, String contentType) {
        String responseJson = """
                You are an invoice parser.
                
                                Return EXACTLY ONE JSON object.
                
                                Do not explain.
                                Do not use markdown.
                                Do not use ```json.
                                Do not return multiple objects.
                IMPORTANT:
                Convert all dates to yyyy-MM-dd format.
                
                Example:
                18/06/26 -> 2026-06-18
                
                Return format:
                {
                  "vendorName":"",
                  "invoiceNumber":"",
                  "purchaseOrderNumber":"",
                  "recipientGstin":"",
                  "gstNumber":"",
                  "totalAmount":0,
                  "taxAmount":0,
                  "subtotal":0,
                  "cgstAmount":0,
                  "sgstAmount":0,
                  "igstAmount":0,
                  "gstRate":0,
                  "invoiceDate":"",
                  "currency":"",
                  "paymentMethod":"",
                  "hsnSac":"",
                  "qrCode":"",
                  "irn":"",
                  "confidenceScore":0.0,
                  }
                  DOCUMENT:
                
                  %s""".formatted(content);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        String body = """
                {
                "model":"llama-3.1-8b-instant",
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
                }""".formatted(objectMapper.writeValueAsString(responseJson));
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange("https://api.groq.com/openai/v1/chat/completions",
                HttpMethod.POST,
                entity,
                String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        String json = root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
        return objectMapper.readValue(
                json,
                ExtractedData.class
        );

    }
}
