package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.Entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Registra la decisión individual de cada jurado sobre un documento específico.
 * Permite rastrear el voto de cada jurado (PRIMARY_EXAMINER_1, PRIMARY_EXAMINER_2, TIEBREAKER_EXAMINER)
 * sobre cada documento de la modalidad.
 */
@Entity
@Table(
    name = "examiner_document_reviews",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"student_document_id", "examiner_id"},
        name = "uk_examiner_document_review"
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminerDocumentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_document_id", nullable = false)
    private StudentDocument studentDocument;

    @ManyToOne(optional = false)
    @JoinColumn(name = "examiner_id", nullable = false)
    private User examiner;

    /**
     * Decisión del jurado: ACCEPTED, CORRECTIONS_REQUESTED, REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private ExaminerDocumentDecision decision;

    @Column(length = 3000)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime reviewedAt;

    /**
     * Indica si este es un voto de desempate
     */
    @Column(nullable = false)
    private Boolean isTiebreakerVote;
}


