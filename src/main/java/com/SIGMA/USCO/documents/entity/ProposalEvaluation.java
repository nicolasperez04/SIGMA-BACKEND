package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proposal_evaluations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(optional = false)
    @JoinColumn(name = "student_document_id")
    private StudentDocument studentDocument;


    @ManyToOne(optional = false)
    @JoinColumn(name = "examiner_id")
    private User examiner;


    @Enumerated(EnumType.STRING)
    @Column(name = "summary", nullable = false)
    private ProposalAspectGrade summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "background_justification", nullable = false)
    private ProposalAspectGrade backgroundJustification;

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_statement", nullable = false)
    private ProposalAspectGrade problemStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "objectives", nullable = false)
    private ProposalAspectGrade objectives;

    @Enumerated(EnumType.STRING)
    @Column(name = "methodology", nullable = false)
    private ProposalAspectGrade methodology;

    @Enumerated(EnumType.STRING)
    @Column(name = "bibliography_references", nullable = false)
    private ProposalAspectGrade bibliographyReferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_organization", nullable = false)
    private ProposalAspectGrade documentOrganization;


    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;
}
