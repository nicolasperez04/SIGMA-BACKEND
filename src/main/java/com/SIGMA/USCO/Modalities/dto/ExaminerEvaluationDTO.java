package com.SIGMA.USCO.Modalities.dto;

import com.SIGMA.USCO.Modalities.Entity.enums.ExaminerDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminerEvaluationDTO {


    @NotNull(message = "La calificación es obligatoria")
    @DecimalMin(value = "0.0", message = "La calificación mínima es 0.0")
    @DecimalMax(value = "5.0", message = "La calificación máxima es 5.0")
    private Double grade;


    @NotNull(message = "La decisión es obligatoria")
    private ExaminerDecision decision;


    @NotBlank(message = "Las observaciones son obligatorias")
    @Size(min = 10, max = 2000, message = "Las observaciones deben tener entre 10 y 2000 caracteres")
    private String observations;

    private java.time.LocalDateTime evaluationDate;
}
