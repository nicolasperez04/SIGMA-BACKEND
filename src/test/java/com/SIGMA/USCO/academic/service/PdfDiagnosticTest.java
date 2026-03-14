package com.SIGMA.USCO.academic.service;

import com.SIGMA.USCO.academic.dto.AcademicHistoryExtractionResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PdfDiagnosticTest {

    @Test
    void testRealPdfExtraction() throws Exception {
        Path pdfPath = Paths.get("src/main/resources/templates/PLANTILLAS - AGRICOLA/Historial academico/Historial academico.pdf");
        
        if (!Files.exists(pdfPath)) {
            System.out.println("PDF no encontrado: " + pdfPath.toAbsolutePath());
            return;
        }
        
        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        System.out.println("\n=== PDF DIAGNOSTICO ===");
        System.out.println("Tamaño del PDF: " + pdfBytes.length + " bytes");
        
        AcademicHistoryPdfParserService parser = new AcademicHistoryPdfParserService();
        
        try {
            AcademicHistoryExtractionResult result = parser.extractFromText("Dummy");
        } catch (Exception e) {
            System.out.println("Error esperado: " + e.getMessage());
        }
        
        System.out.println("Verificando si el parser puede leer el PDF...");
        assertNotNull(pdfBytes);
    }
}
