package com.SIGMA.USCO.academic.entity;

import com.SIGMA.USCO.Users.Entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "academic_history_pdfs")
public class AcademicHistoryPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_profile_id")
    private StudentProfile studentProfile;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User uploadedBy;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    // Datos extraídos del PDF para búsqueda y auditoría
    @Column(name = "extracted_program_name")
    private String extractedProgramName;

    @Column(name = "extracted_approved_credits")
    private Long extractedApprovedCredits;

    @Column(name = "extracted_total_credits")
    private Long extractedTotalCredits;

    @Column(name = "extracted_gpa")
    private Double extractedGpa;

    // Metadata
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

