package com.SIGMA.USCO.documents.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalEvaluationResponseDTO {

    private Long evaluationId;
    private Long studentDocumentId;
    private String documentName;

    private String evaluatorName;
    private String evaluatorEmail;

    private String summary;
    private String backgroundJustification;
    private String problemStatement;
    private String objectives;
    private String methodology;
    private String bibliographyReferences;
    private String documentOrganization;

    private String observations;
    private LocalDateTime evaluatedAt;
}
