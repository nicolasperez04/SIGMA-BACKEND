package com.SIGMA.USCO.academic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicHistoryExtractionResult {

    private String programName;
    private Long approvedCredits;
    private Long totalCreditsInPdf;
    private Double gpa;
}

