package com.verivoice.server.dto;

import com.verivoice.server.embeddable.ExtractedData;
import com.verivoice.server.entity.Document;
import com.verivoice.server.verification.VerificationCheck;
import com.verivoice.server.verification.VerificationClassification;
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
public class DocumentDto {
    private String id;
    private String fileName;
    private String filePath;
    private String contentType;
    private String fileHash;
    private LocalDateTime uploadDate;
    private Document.DocumentStatus status;
    private ExtractedData extractedData;
    private List<String> anomalies;
    private List<VerificationCheck> verificationChecks;
    private String rawLlmResponse;
    private String extractedText;
    private Double riskScore;
    private Integer verificationScore;
    private VerificationClassification verificationStatus;
}
