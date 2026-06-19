package com.verivoice.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "gst_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GstCache {
    @Id
    private String gstin;
    private String legalName;
    private String status;
    private LocalDateTime lastVerified;
}
