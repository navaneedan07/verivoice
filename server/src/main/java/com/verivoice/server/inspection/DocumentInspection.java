package com.verivoice.server.inspection;

import java.util.List;

public record DocumentInspection(
        boolean pdf,
        boolean qrPresent,
        String qrPayload,
        boolean digitallySignedPdf,
        boolean metadataSuggestsEditing,
        boolean multipleRevisions,
        int fontCount,
        boolean suspiciousFontUsage,
        boolean amountTypographyChecked,
        boolean amountTypographyAnomaly,
        boolean imageOnlyPdf,
        List<String> forensicNotes,
        String sha256
) {
    public static DocumentInspection unsupported() {
        return unsupported(null);
    }

    public static DocumentInspection unsupported(String sha256) {
        return new DocumentInspection(
                false, false, null, false, false, false,
                0, false, false, false, false,
                List.of("Forensic PDF checks do not apply to this file type."),
                sha256
        );
    }
}
