package com.verivoice.server.inspection;

public record DocumentInspection(
        boolean qrPresent,
        String qrPayload,
        String sha256
) {
    public static DocumentInspection unsupported() {
        return unsupported(null);
    }

    public static DocumentInspection unsupported(String sha256) {
        return new DocumentInspection(
            false, null, sha256
        );
    }
}
