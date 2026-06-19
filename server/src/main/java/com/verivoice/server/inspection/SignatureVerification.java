package com.verivoice.server.inspection;

public record SignatureVerification(Status status, String detail) {
    public enum Status {
        VERIFIED, INVALID, NOT_CONFIGURED, UNSUPPORTED
    }
}
