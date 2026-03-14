package com.SIGMA.USCO.academic.repository;

import com.SIGMA.USCO.academic.entity.AcademicHistoryPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicHistoryPdfRepository extends JpaRepository<AcademicHistoryPdf, Long> {

    /**
     * Obtener todos los PDFs de historial académico de un estudiante
     */
    List<AcademicHistoryPdf> findByStudentProfileUserId(Long userId);

    /**
     * Obtener el PDF más reciente de un estudiante
     */
    Optional<AcademicHistoryPdf> findFirstByStudentProfileUserIdOrderByUploadDateDesc(Long userId);

    /**
     * Obtener todos los PDFs de historial académico de una modalidad de estudiante
     */
    @Query("SELECT ahp FROM AcademicHistoryPdf ahp " +
           "JOIN ahp.studentProfile sp " +
           "WHERE sp.user.id IN (" +
           "  SELECT smm.student.id FROM StudentModalityMember smm " +
           "  WHERE smm.studentModality.id = :studentModalityId " +
           "  AND smm.status = 'ACTIVE'" +
           ")")
    List<AcademicHistoryPdf> findByStudentModalityId(@Param("studentModalityId") Long studentModalityId);

    /**
     * Obtener PDF por su ID
     */
    Optional<AcademicHistoryPdf> findById(Long id);

    /**
     * Verificar si un estudiante ya subió un PDF de historial académico
     */
    boolean existsByStudentProfileUserId(Long userId);
}



