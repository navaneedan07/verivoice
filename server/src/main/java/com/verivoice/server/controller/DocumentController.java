package com.verivoice.server.controller;

import com.verivoice.server.dto.DocumentDto;
import com.verivoice.server.dto.StoredDocumentFile;
import com.verivoice.server.service.DocumentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService docService;

    public DocumentController(DocumentService docService) {
        this.docService = docService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentDto> uploadDocument(@RequestParam("file") MultipartFile file) throws Exception{
        return ResponseEntity.ok(docService.processDocument(file));
    }

    @GetMapping("{id}")
    public ResponseEntity<DocumentDto> getDocumentById(@PathVariable("id") String docId) {
        DocumentDto docDto = docService.getDocumentById(docId);
        return ResponseEntity.ok(docDto);
    }

    @GetMapping("{id}/file")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable("id") String docId) {
        StoredDocumentFile file = docService.getDocumentFile(docId);
        MediaType mediaType;
        try {
            mediaType = file.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(file.contentType());
        } catch (IllegalArgumentException ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.fileName() == null ? "invoice" : file.fileName())
                                .build()
                                .toString()
                )
                .body(file.content());
    }
}
