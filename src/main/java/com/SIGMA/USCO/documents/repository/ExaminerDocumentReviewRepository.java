package com.SIGMA.USCO.documents.repository;

import com.SIGMA.USCO.documents.entity.ExaminerDocumentReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExaminerDocumentReviewRepository extends JpaRepository<ExaminerDocumentReview, Long> {

    Optional<ExaminerDocumentReview> findByStudentDocumentIdAndExaminerId(Long studentDocumentId, Long examinerId);

    List<ExaminerDocumentReview> findByStudentDocumentId(Long studentDocumentId);

    boolean existsByStudentDocumentIdAndExaminerId(Long studentDocumentId, Long examinerId);

    /**
     * Devuelve todas las reviews de los jurados de una modalidad para un documento específico
     */
    @Query("SELECT edr FROM ExaminerDocumentReview edr " +
           "WHERE edr.studentDocument.id = :studentDocumentId")
    List<ExaminerDocumentReview> findAllByStudentDocumentId(@Param("studentDocumentId") Long studentDocumentId);

    /**
     * Cuenta las reviews con una decisión específica para un documento
     */
    @Query("SELECT COUNT(edr) FROM ExaminerDocumentReview edr " +
           "WHERE edr.studentDocument.id = :studentDocumentId " +
           "AND edr.decision = :decision")
    long countByStudentDocumentIdAndDecision(
            @Param("studentDocumentId") Long studentDocumentId,
            @Param("decision") com.SIGMA.USCO.documents.entity.ExaminerDocumentDecision decision);
}

