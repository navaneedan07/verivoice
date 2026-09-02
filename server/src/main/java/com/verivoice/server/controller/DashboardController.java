package com.verivoice.server.controller;

import com.verivoice.server.dto.DashboardStats;
import com.verivoice.server.dto.DocumentDto;
import com.verivoice.server.entity.Document;
import com.verivoice.server.mapper.DocumentMapper;
import com.verivoice.server.repository.DocumentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DocumentRepository documentRepository;

    public DashboardController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardStats> getDashboardStats() {
        long totalInvoices = documentRepository.count();
        long verified = documentRepository.countByStatus(Document.DocumentStatus.APPROVED);
        long flagged = documentRepository.countByStatus(Document.DocumentStatus.FLAGGED);
        long pendingReview = documentRepository.countByStatus(Document.DocumentStatus.NEEDS_REVIEW);

        List<Document> recent = documentRepository.findTop5ByOrderByUploadDateDesc();
        List<DocumentDto> recentDtos = recent.stream()
                .map(DocumentMapper::mapToDocumentDto)
                .toList();

        DashboardStats stats = new DashboardStats(
                totalInvoices,
                verified,
                flagged,
                pendingReview,
                recentDtos
        );

        return ResponseEntity.ok(stats);
    }
}
