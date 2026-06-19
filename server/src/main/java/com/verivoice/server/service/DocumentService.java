package com.verivoice.server.service;

import com.verivoice.server.dto.DocumentDto;
import com.verivoice.server.dto.StoredDocumentFile;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    DocumentDto processDocument(MultipartFile file) throws Exception;
    DocumentDto getDocumentById(String docId);
    StoredDocumentFile getDocumentFile(String docId);
}
