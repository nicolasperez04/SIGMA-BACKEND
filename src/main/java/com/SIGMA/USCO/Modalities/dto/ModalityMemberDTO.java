package com.SIGMA.USCO.Modalities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para representar un miembro de una modalidad grupal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModalityMemberDTO {

    private Long memberId;
    private Long studentId;
    private String studentName;
    private String studentLastName;
    private String studentEmail;
    private String studentCode;
    private Boolean isLeader;
    private String status;
    private LocalDateTime joinedAt;
    private Integer approvedCredits;
    private Double gpa;
    private String semester;

}
