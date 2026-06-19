package com.verivoice.server.service.impl;

import com.verivoice.server.dto.DocumentDto;
import com.verivoice.server.dto.StoredDocumentFile;
import com.verivoice.server.embeddable.ExtractedData;
import com.verivoice.server.entity.Document;
import com.verivoice.server.exception.DocumentNotFoundException;
import com.verivoice.server.inspection.DocumentInspection;
import com.verivoice.server.mapper.DocumentMapper;
import com.verivoice.server.repository.DocumentRepository;
import com.verivoice.server.service.DocumentService;
import com.verivoice.server.verification.CheckStatus;
import com.verivoice.server.verification.VerificationOutcome;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository docRepo;
    private final GroqService groqService;
    private final ValidationService validationService;
    private final DocumentExtractionService extractionService;
    private final DocumentInspectionService inspectionService;
    private final GstRegistrySyncService gstRegistrySyncService;
    private final VendorHistoryService vendorHistoryService;

    public DocumentServiceImpl(
            DocumentRepository docRepo,
            GroqService groqService,
            ValidationService validationService,
            DocumentExtractionService extractionService,
            DocumentInspectionService inspectionService,
            GstRegistrySyncService gstRegistrySyncService,
            VendorHistoryService vendorHistoryService
    ) {
        this.docRepo = docRepo;
        this.groqService = groqService;
        this.validationService = validationService;
        this.extractionService = extractionService;
        this.inspectionService = inspectionService;
        this.gstRegistrySyncService = gstRegistrySyncService;
        this.vendorHistoryService = vendorHistoryService;
    }
    @Override
    public DocumentDto processDocument(MultipartFile file) throws Exception {
        Document doc = new Document();
        doc.setFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setSourceFile(file.getBytes());
        doc.setStatus(
                Document.DocumentStatus.PROCESSING
        );
        String content = extractionService.extractContent(file);
        ExtractedData data = groqService.analyzeInvoice(content, file.getContentType());
        DocumentInspection inspection = inspectionService.inspect(file, data);
        doc.setFileHash(inspection.sha256());
        if (data.getGstNumber() != null) {
            data.setGstNumber(data.getGstNumber().trim().toUpperCase());
            gstRegistrySyncService.refreshIfConfigured(data.getGstNumber());
        }
        if (inspection.qrPayload() != null) {
            data.setQrCode(inspection.qrPayload());
        }
        doc.setExtractedData(data);
        doc.setExtractedText(content);
        VerificationOutcome outcome = validationService.validate(data, inspection);
        doc.setAnomalies(outcome.anomalies());
        doc.setVerificationChecks(outcome.checks());
        doc.setVerificationScore(outcome.score());
        doc.setVerificationStatus(outcome.classification());
        doc.setRiskScore(100d - outcome.score());
        doc.setFraudDetected(outcome.checks().stream()
                .anyMatch(check -> "FRAUD_ANALYSIS".equals(check.getLayer())
                        && !"MANDATORY_FIELDS".equals(check.getCode())
                        && check.getStatus() == CheckStatus.FAILED));
        doc.setStatus(mapDocumentStatus(outcome));
        Document savedDoc = docRepo.save(doc);
        vendorHistoryService.record(data, outcome);
        return DocumentMapper.mapToDocumentDto(savedDoc);
    }

    @Override
    public DocumentDto getDocumentById(String docId) {
        Document doc = docRepo.findById(docId).orElseThrow(()-> new DocumentNotFoundException("Document doesn't exist"));
        return DocumentMapper.mapToDocumentDto(doc);
    }

    @Override
    public StoredDocumentFile getDocumentFile(String docId) {
        Document doc = docRepo.findById(docId)
                .orElseThrow(() -> new DocumentNotFoundException("Document doesn't exist"));
        return new StoredDocumentFile(doc.getFileName(), doc.getContentType(), doc.getSourceFile());
    }

    private Document.DocumentStatus mapDocumentStatus(VerificationOutcome outcome) {
        return switch (outcome.classification()) {
            case VERIFIED -> Document.DocumentStatus.APPROVED;
            case LOW_RISK, REVIEW_REQUIRED -> Document.DocumentStatus.NEEDS_REVIEW;
            case HIGH_RISK -> Document.DocumentStatus.FLAGGED;
        };
    }
}
