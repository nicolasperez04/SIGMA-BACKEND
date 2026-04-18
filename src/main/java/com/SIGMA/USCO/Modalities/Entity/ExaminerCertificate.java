package com.SIGMA.USCO.Modalities.Entity;

import com.SIGMA.USCO.Modalities.Entity.enums.CertificateStatus;
import com.SIGMA.USCO.Users.Entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Acta de participación del jurado en la modalidad de grado.
 * Registra la participación, evaluaciones y culminación del proceso por parte del jurado.
 */
@Entity
@Table(name = "examiner_certificates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminerCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_modality_id", nullable = false)
    private StudentModality studentModality;

    @ManyToOne(optional = false)
    @JoinColumn(name = "examiner_id", nullable = false)
    private User examiner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "defense_examiner_id", nullable = false)
    private DefenseExaminer defenseExaminer;

    @Column(nullable = false, unique = true)
    private String certificateNumber;

    @Column(nullable = false)
    private LocalDateTime issueDate;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status;

    @Column
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

