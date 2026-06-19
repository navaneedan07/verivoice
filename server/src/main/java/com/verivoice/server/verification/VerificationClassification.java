package com.verivoice.server.verification;

public enum VerificationClassification {
    VERIFIED,
    LOW_RISK,
    REVIEW_REQUIRED,
    HIGH_RISK;

    public static VerificationClassification fromScore(int score) {
        if (score >= 90) {
            return VERIFIED;
        }
        if (score >= 70) {
            return LOW_RISK;
        }
        if (score >= 40) {
            return REVIEW_REQUIRED;
        }
        return HIGH_RISK;
    }
}
