package com.SIGMA.USCO.academic.service;

import com.SIGMA.USCO.academic.dto.AcademicHistoryExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcademicHistoryPdfParserServiceTest {

    private final AcademicHistoryPdfParserService parserService = new AcademicHistoryPdfParserService();

    @Test
    void shouldExtractAcademicDataFromTemplateText() {
        String templateText = """
                Programa: INGENIERIA DE SOFTWARE NEIVA
                Creditos aprobados: 142 de 165
                Puntaje calificado ponderado (con perdidas): 4.21
                """;

        AcademicHistoryExtractionResult result = parserService.extractFromText(templateText);

        assertEquals("INGENIERIA DE SOFTWARE NEIVA", result.getProgramName());
        assertEquals(142L, result.getApprovedCredits());
        assertEquals(165L, result.getTotalCreditsInPdf());
        assertEquals(4.21, result.getGpa());
    }

    @Test
    void shouldExtractAcademicDataFromTableLikeTextWithoutColons() {
        String tableText = """
                INFORMACION DEL ESTUDIANTE
                Programa INGENIERIA DE SOFTWARE NEIVA
                Registro SNIES programa 102526
                Plan 0141192
                Creditos Aprobados 142 de 165
                Puntaje calificado ponderado (con perdidas) 4.21
                """;

        AcademicHistoryExtractionResult result = parserService.extractFromText(tableText);

        assertEquals("INGENIERIA DE SOFTWARE NEIVA", result.getProgramName());
        assertEquals(142L, result.getApprovedCredits());
        assertEquals(165L, result.getTotalCreditsInPdf());
        assertEquals(4.21, result.getGpa());
    }

    @Test
    void shouldExtractFromRealLikeStudentText() {
        String raw = """
                INFORMACION DEL ESTUDIANTE
                Nombre NICOLAS PEREZ PEREZ
                Codigo del estudiante 20221204357
                Tipo de Identificacion CEDULA DE CIUDADANIA
                Identificacion 1003896330
                Edad 22
                Email u20221204357@usco.edu.co
                Estrato 2
                Programa INGENIERIA DE SOFTWARE NEIVA
                Registro SNIES programa 102526
                Plan 0141192
                Creditos Aprobados 142 de 165
                Creditos Cursados 154
                Puntaje calificado ponderado (con perdidas) 4.21
                """;

        AcademicHistoryExtractionResult result = parserService.extractFromText(raw);

        assertEquals("INGENIERIA DE SOFTWARE NEIVA", result.getProgramName());
        assertEquals(142L, result.getApprovedCredits());
        assertEquals(165L, result.getTotalCreditsInPdf());
        assertEquals(4.21, result.getGpa());
    }

    @Test
    void shouldExtractAcademicDataFromRealPdfFile() throws IOException {
        Path pdfPath = Path.of(
                "src/main/resources/templates/PLANTILLAS - AGRICOLA/Historial academico/Historial academico.pdf"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Historial academico.pdf",
                "application/pdf",
                Files.readAllBytes(pdfPath)
        );

        AcademicHistoryExtractionResult result = parserService.extract(file);

        assertEquals("INGENIERIA DE SOFTWARE NEIVA", result.getProgramName());
        assertEquals(142L, result.getApprovedCredits());
        assertEquals(165L, result.getTotalCreditsInPdf());
        assertEquals(4.21, result.getGpa());
    }
}
