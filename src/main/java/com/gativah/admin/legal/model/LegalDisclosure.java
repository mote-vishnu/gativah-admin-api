package com.gativah.admin.legal.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single disclosure of data made under a legal request (legal_disclosure, V90). */
@Entity
@Table(name = "legal_disclosure")
@Getter
@Setter
@NoArgsConstructor
public class LegalDisclosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "disclosed_by", nullable = false)
    private Long disclosedBy;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "data_categories", nullable = false, length = 500)
    private String dataCategories;

    @Column(nullable = false, length = 2000)
    private String justification;

    @Column(name = "disclosed_at", nullable = false)
    private LocalDateTime disclosedAt;

    @PrePersist
    void onCreate() {
        if (disclosedAt == null) {
            disclosedAt = LocalDateTime.now();
        }
    }
}
