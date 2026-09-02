package com.verivoice.server.repository;

import com.verivoice.server.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    long countByStatus(Document.DocumentStatus status);

    List<Document> findTop5ByOrderByUploadDateDesc();

    boolean existsByExtractedDataGstNumberAndExtractedDataInvoiceNumber(
            String gstNumber,
            String invoiceNumber
    );

    boolean existsByExtractedDataGstNumberAndExtractedDataTotalAmountAndExtractedDataInvoiceDate(
            String gstNumber,
            Double totalAmount,
            LocalDate invoiceDate
    );

    boolean existsByExtractedDataGstNumber(String gstNumber);
    boolean existsByFileHash(String fileHash);

    Optional<Document> findTopByExtractedDataGstNumberOrderByExtractedDataInvoiceDateDescUploadDateDesc(
            String gstNumber
    );
}
