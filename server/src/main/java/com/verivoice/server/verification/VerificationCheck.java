package com.verivoice.server.verification;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCheck {
    private String layer;
    private String code;
    private String description;

    @Enumerated(EnumType.STRING)
    private CheckStatus status;

    @Column(length = 1000)
    private String detail;
    private int scoreAwarded;
}
