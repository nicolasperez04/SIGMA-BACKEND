package com.SIGMA.USCO.documents.dto;

import com.SIGMA.USCO.documents.entity.ProposalAspectGrade;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalEvaluationRequest {

    @NotNull(message = "La calificación del resumen es obligatoria")
    private ProposalAspectGrade summary;

    @NotNull(message = "La calificación de antecedentes y justificación es obligatoria")
    private ProposalAspectGrade backgroundJustification;

    @NotNull(message = "La calificación de formulación del problema es obligatoria")
    private ProposalAspectGrade problemStatement;

    @NotNull(message = "La calificación de objetivos es obligatoria")
    private ProposalAspectGrade objectives;

    @NotNull(message = "La calificación de metodología es obligatoria")
    private ProposalAspectGrade methodology;

    @NotNull(message = "La calificación de bibliografía o referencias es obligatoria")
    private ProposalAspectGrade bibliographyReferences;

    @NotNull(message = "La calificación de organización del documento es obligatoria")
    private ProposalAspectGrade documentOrganization;


}
