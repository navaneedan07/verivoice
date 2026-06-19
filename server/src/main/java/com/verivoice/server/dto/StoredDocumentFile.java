package com.verivoice.server.dto;

public record StoredDocumentFile(String fileName, String contentType, byte[] content) {
}
