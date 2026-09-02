package com.verivoice.server.service.impl;

import com.verivoice.server.inspection.SignatureVerification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class IrpSignatureService {
    private final List<Path> certificatePaths;

    public IrpSignatureService(
            @Value("${verification.irp-certificates:}") String configuredPaths
    ) {
        this.certificatePaths = new ArrayList<>();
        if (configuredPaths != null && !configuredPaths.isBlank()) {
            for (String value : configuredPaths.split(",")) {
                if (!value.isBlank()) {
                    certificatePaths.add(Path.of(value.trim()));
                }
            }
        }
    }

    public SignatureVerification verify(String signedPayload) {
        if (signedPayload == null || signedPayload.isBlank()) {
            return new SignatureVerification(
                    SignatureVerification.Status.UNSUPPORTED,
                    "No signed QR payload is available."
            );
        }
        String[] parts = signedPayload.split("\\.");
        if (parts.length != 3) {
            return new SignatureVerification(
                    SignatureVerification.Status.UNSUPPORTED,
                    "Decoded QR payload is not a signed GST e-invoice JWS; only plain QR data was found."
            );
        }
        if (certificatePaths.isEmpty()) {
            return new SignatureVerification(
                    SignatureVerification.Status.NOT_CONFIGURED,
                    "No issuing IRP X.509 certificate has been configured."
            );
        }

        byte[] signedBytes = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
        byte[] signatureBytes;
        try {
            signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException exception) {
            return new SignatureVerification(
                    SignatureVerification.Status.INVALID,
                    "QR signature is not valid Base64URL data."
            );
        }

        for (Path path : certificatePaths) {
            try {
                X509Certificate certificate;
                try (var input = Files.newInputStream(path)) {
                    certificate = (X509Certificate) CertificateFactory
                            .getInstance("X.509")
                            .generateCertificate(input);
                }
                Signature verifier = Signature.getInstance("SHA256withRSA");
                verifier.initVerify(certificate.getPublicKey());
                verifier.update(signedBytes);
                if (verifier.verify(signatureBytes)) {
                    return new SignatureVerification(
                            SignatureVerification.Status.VERIFIED,
                            "QR JWS signature matches configured IRP certificate " + path + "."
                    );
                }
            } catch (Exception ignored) {
                // Try every configured current/previous IRP certificate.
            }
        }
        return new SignatureVerification(
                SignatureVerification.Status.INVALID,
                "QR signature did not match any configured IRP certificate."
        );
    }
}
