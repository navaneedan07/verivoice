package com.verivoice.server.verification;

import java.util.List;

public record VerificationOutcome(
        int score,
        VerificationClassification classification,
        List<VerificationCheck> checks,
        List<String> anomalies
) {
}
