package com.verivoice.server.mapper;

import com.verivoice.server.dto.DocumentDto;
import com.verivoice.server.entity.Document;

public class DocumentMapper {
    public static DocumentDto mapToDocumentDto(Document doc) {
        if(doc == null) return null;
        DocumentDto dto = new DocumentDto();

        dto.setId(doc.getId());
        dto.setFileName(doc.getFileName());
        dto.setFilePath(doc.getFilePath());
        dto.setContentType(doc.getContentType());
        dto.setFileHash(doc.getFileHash());
        dto.setUploadDate(doc.getUploadDate());
        dto.setStatus(doc.getStatus());
        dto.setExtractedData(doc.getExtractedData());
        dto.setAnomalies(doc.getAnomalies());
        dto.setVerificationChecks(doc.getVerificationChecks());
        dto.setRawLlmResponse(doc.getRawLlmResponse());
        dto.setExtractedText(doc.getExtractedText());
        dto.setRiskScore(doc.getRiskScore());
        dto.setVerificationScore(doc.getVerificationScore());
        dto.setVerificationStatus(doc.getVerificationStatus());
        dto.setFraudDetected(doc.getFraudDetected());

        return dto;
    }

    public static Document mapToDocument(DocumentDto dto) {
        if(dto == null) return null;
        Document doc = new Document();

        doc.setId(dto.getId());
        doc.setFileName(dto.getFileName());
        doc.setFilePath(dto.getFilePath());
        doc.setContentType(dto.getContentType());
        doc.setFileHash(dto.getFileHash());
        doc.setUploadDate(dto.getUploadDate());
        doc.setStatus(dto.getStatus());
        doc.setExtractedData(dto.getExtractedData());
        doc.setAnomalies(dto.getAnomalies());
        doc.setVerificationChecks(dto.getVerificationChecks());
        doc.setRawLlmResponse(dto.getRawLlmResponse());
        doc.setExtractedText(dto.getExtractedText());
        doc.setRiskScore(dto.getRiskScore());
        doc.setVerificationScore(dto.getVerificationScore());
        doc.setVerificationStatus(dto.getVerificationStatus());
        doc.setFraudDetected(dto.getFraudDetected());

        return doc;
    }
}
