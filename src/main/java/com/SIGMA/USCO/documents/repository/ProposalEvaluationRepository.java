package com.SIGMA.USCO.documents.repository;

import com.SIGMA.USCO.documents.entity.ProposalEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProposalEvaluationRepository extends JpaRepository<ProposalEvaluation, Long> {

    List<ProposalEvaluation> findByStudentDocumentId(Long studentDocumentId);

    Optional<ProposalEvaluation> findByStudentDocumentIdAndExaminerId(Long studentDocumentId, Long examinerId);

    boolean existsByStudentDocumentIdAndExaminerId(Long studentDocumentId, Long examinerId);
}
