package com.verivoice.server.entity;

import com.verivoice.server.embeddable.ExtractedData;
import com.verivoice.server.verification.VerificationCheck;
import com.verivoice.server.verification.VerificationClassification;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices")

public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String fileName;
    private String filePath;
    private String contentType;
    @Column(length = 64)
    private String fileHash;
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(columnDefinition = "bytea")
    private byte[] sourceFile;
    private LocalDateTime uploadDate = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.PENDING;
    @Embedded
    private ExtractedData extractedData;
    @ElementCollection
    @CollectionTable(name = "invoice_anomalies", joinColumns = @JoinColumn(name = "invoice_id"))
    @Column(length = 1000)
    private List<String> anomalies;
    @ElementCollection
    @CollectionTable(name = "invoice_verification_checks", joinColumns = @JoinColumn(name = "invoice_id"))
    private List<VerificationCheck> verificationChecks;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawLlmResponse;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String extractedText;
    private Double riskScore;
    private Integer verificationScore;
    @Enumerated(EnumType.STRING)
    private VerificationClassification verificationStatus;
    private Boolean fraudDetected;
    public enum DocumentStatus {
        PENDING, PROCESSING, NEEDS_REVIEW, FLAGGED, APPROVED, REJECTED
    }
}
