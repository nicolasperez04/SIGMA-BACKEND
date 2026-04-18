package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.Entity.*;
import com.SIGMA.USCO.Modalities.Entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.Repository.ExaminerCertificateRepository;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Servicio para generar actas de participación de jurados en modalidades de grado.
 * Cada jurado recibe un acta que certifica su participación en todas las etapas del proceso
 * y el registro de su evaluación final en la sustentación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExaminerCertificatePdfService {

    private final ExaminerCertificateRepository certificateRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final StudentModalityRepository studentModalityRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ── Paleta institucional ──────────────────────────────────────────────────
    private static final BaseColor INST_RED    = new BaseColor(143, 30, 30);
    private static final BaseColor INST_GOLD   = new BaseColor(180, 140, 60);
    private static final BaseColor ROW_BG_LIGHT = new BaseColor(250, 247, 242);
    private static final BaseColor ROW_BG_ALT  = new BaseColor(245, 240, 230);
    private static final BaseColor GRAY_DARK   = new BaseColor(60, 60, 60);
    private static final BaseColor GRAY_MID    = new BaseColor(110, 110, 110);
    private static final BaseColor BLUE_DARK   = new BaseColor(25, 75, 140);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font FONT_UNIV_NAME   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   13f, INST_RED);
    private static final Font FONT_FACULTY     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11f, GRAY_DARK);
    private static final Font FONT_PROGRAM     = FontFactory.getFont(FontFactory.HELVETICA,         10f, GRAY_DARK);
    private static final Font FONT_TITLE       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   15f, INST_RED);
    private static final Font FONT_ACTA_NUM    = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  9f, GRAY_MID);
    private static final Font FONT_SECTION_HDR = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11f, BaseColor.WHITE);
    private static final Font FONT_LABEL       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9f, GRAY_DARK);
    private static final Font FONT_VALUE       = FontFactory.getFont(FontFactory.HELVETICA,          9f, BaseColor.BLACK);
    private static final Font FONT_BODY        = FontFactory.getFont(FontFactory.HELVETICA,          9f, GRAY_DARK);
    private static final Font FONT_FOOTER      = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  7f, GRAY_MID);
    private static final Font FONT_SIGN_NAME   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    8f, BaseColor.BLACK);
    private static final Font FONT_SIGN_ROLE   = FontFactory.getFont(FontFactory.HELVETICA,          7f, GRAY_MID);
    private static final Font FONT_HIGHLIGHT  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9f, BLUE_DARK);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-CO"));

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Genera una acta de participación para un jurado específico en una modalidad.
     * El acta certifica su participación en todas las etapas y el registro de su evaluación.
     *
     * @param studentModality La modalidad de grado
     * @param defenseExaminer El jurado participante
     * @return El certificado del jurado guardado
     * @throws IOException Si ocurre un error en la generación del PDF
     */
    @Transactional
    public ExaminerCertificate generateExaminerCertificate(StudentModality studentModality, DefenseExaminer defenseExaminer) throws IOException {
        // Refrescar la modalidad desde la BD para obtener el estado más actualizado
        StudentModality refreshedModality = studentModalityRepository.findById(studentModality.getId())
                .orElseThrow(() -> new RuntimeException("Modalidad de grado no encontrada"));
        
        log.info("Generando certificado de jurado. Modalidad ID: {}, Estado actual: {}", 
            refreshedModality.getId(), refreshedModality.getStatus());
        
        // Eliminar certificado previo si existe
        ExaminerCertificate existing = certificateRepository
                .findByModalityAndExaminer(refreshedModality.getId(), defenseExaminer.getExaminer().getId())
                .orElse(null);
        if (existing != null) {
            try {
                Path old = Paths.get(existing.getFilePath());
                if (Files.exists(old)) Files.delete(old);
                log.info("Acta anterior de jurado eliminada: {}", old);
            } catch (IOException ex) {
                log.warn("No se pudo eliminar acta anterior de jurado: {}", ex.getMessage());
            }
            certificateRepository.delete(existing);
            certificateRepository.flush();
            log.info("Registro de acta anterior de jurado eliminado de BD");
        }

        String examinerName = defenseExaminer.getExaminer().getName() + " " + defenseExaminer.getExaminer().getLastName();
        String certNumber = generateCertificateNumber(refreshedModality, defenseExaminer.getExaminer());

        Path outDir = Paths.get(uploadDir, "certificates",
                String.valueOf(refreshedModality.getProgramDegreeModality().getAcademicProgram().getId()),
                "examiners");
        Files.createDirectories(outDir);
        String fileName = "ACTA_JURADO_" + certNumber + "_" + defenseExaminer.getExaminer().getId() + ".pdf";
        Path filePath = outDir.resolve(fileName);

        buildPdf(filePath, refreshedModality, defenseExaminer, certNumber);
        log.info("Acta de jurado generada: {}", filePath);

        String hash = calculateFileHash(filePath);
        ExaminerCertificate cert = ExaminerCertificate.builder()
                .studentModality(refreshedModality)
                .examiner(defenseExaminer.getExaminer())
                .defenseExaminer(defenseExaminer)
                .certificateNumber(certNumber)
                .issueDate(LocalDateTime.now())
                .filePath(filePath.toString())
                .fileHash(hash)
                .status(CertificateStatus.GENERATED)
                .build();
        return certificateRepository.save(cert);
    }

    public Path getCertificatePath(Long modalityId, Long examinerId) {
        ExaminerCertificate cert = certificateRepository
                .findByModalityAndExaminer(modalityId, examinerId)
                .orElseThrow(() -> new RuntimeException("Acta de jurado no encontrada"));
        return Paths.get(cert.getFilePath());
    }

    @Transactional
    public void updateCertificateStatus(Long certificateId, CertificateStatus status) {
        ExaminerCertificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Acta de jurado no encontrada"));
        cert.setStatus(status);
        certificateRepository.save(cert);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Construcción del PDF
    // ─────────────────────────────────────────────────────────────────────────

    private void buildPdf(Path filePath, StudentModality sm, DefenseExaminer examiner, String certNumber) {
        try {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 50);
            PdfWriter.getInstance(doc, new FileOutputStream(filePath.toFile()));
            doc.open();

            String facultyName = sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName();
            String programName = sm.getProgramDegreeModality().getAcademicProgram().getName();

            // ── 1. CABECERA INSTITUCIONAL
            addInstitutionalHeader(doc, facultyName, programName);

            // ── 2. LÍNEAS DIVISORAS
            addRedLine(doc);
            addSpacing(doc, 6f);

            // ── 3. TÍTULO Y NÚMERO
            Paragraph title = new Paragraph("ACTA DE PARTICIPACIÓN DE JURADO EN MODALIDAD DE GRADO", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            Paragraph actaNum = new Paragraph("No. " + certNumber, FONT_ACTA_NUM);
            actaNum.setAlignment(Element.ALIGN_CENTER);
            actaNum.setSpacingAfter(4f);
            doc.add(actaNum);

            Paragraph issueDate = new Paragraph(
                    "Neiva, " + LocalDateTime.now().format(DATE_FMT), FONT_BODY);
            issueDate.setAlignment(Element.ALIGN_RIGHT);
            issueDate.setSpacingAfter(10f);
            doc.add(issueDate);

            addGoldLine(doc);
            addSpacing(doc, 8f);

            // ── 4. CUERPO CERTIFICATORIO
            addCertificationBody(doc, sm, examiner);
            addSpacing(doc, 12f);

            // ── 5. DATOS DEL JURADO
            addSectionHeader(doc, "I. DATOS DEL JURADO");
            addSpacing(doc, 4f);
            addExaminerDataTable(doc, examiner);
            addSpacing(doc, 10f);

            // ── 6. INFORMACIÓN DE LA MODALIDAD
            addSectionHeader(doc, "II. INFORMACIÓN DE LA MODALIDAD");
            addSpacing(doc, 4f);
            addModalityTable(doc, sm);
            addSpacing(doc, 10f);

            // ── 7. PARTICIPACIÓN Y EVALUACIÓN
            addSectionHeader(doc, "III. PARTICIPACIÓN Y EVALUACIÓN");
            addSpacing(doc, 4f);
            addParticipationTable(doc, sm, examiner);
            addSpacing(doc, 10f);

            // ── 8. DECLARACIÓN DE CUMPLIMIENTO
            addComplianceDeclaration(doc, examiner);
            addSpacing(doc, 20f);

            // ── 9. FIRMA
            addSignatureSection(doc, examiner);
            addSpacing(doc, 16f);

            // ── 10. PIE DE PÁGINA
            addGoldLine(doc);
            addSpacing(doc, 6f);
            addFooter(doc, certNumber);

            doc.close();
            log.info("Acta de jurado generada exitosamente: {}", filePath);
        } catch (DocumentException | IOException e) {
            log.error("Error generando acta de jurado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el acta de jurado", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bloques de construcción
    // ─────────────────────────────────────────────────────────────────────────

    private void addInstitutionalHeader(Document doc, String facultyName, String programName)
            throws DocumentException, IOException {

        PdfPTable headerTable = new PdfPTable(new float[]{1.5f, 5f});
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(4f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(2f);

        try {
            ClassPathResource logoResource = new ClassPathResource("templates/logo ingenieria.png");
            try (InputStream logoStream = logoResource.getInputStream()) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(90f, 70f);
                logo.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo institucional: {}", e.getMessage());
            logoCell.addElement(new Paragraph(" ", FONT_BODY));
        }

        headerTable.addCell(logoCell);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.setPaddingLeft(10f);

        Paragraph univName = new Paragraph("UNIVERSIDAD SURCOLOMBIANA", FONT_UNIV_NAME);
        univName.setAlignment(Element.ALIGN_LEFT);
        univName.setSpacingAfter(2f);

        Paragraph fac = new Paragraph(facultyName.toUpperCase(), FONT_FACULTY);
        fac.setAlignment(Element.ALIGN_LEFT);
        fac.setSpacingAfter(2f);

        Paragraph prog = new Paragraph(programName, FONT_PROGRAM);
        prog.setAlignment(Element.ALIGN_LEFT);
        prog.setSpacingAfter(2f);

        Paragraph slogan = new Paragraph("Sistema de Información y Gestión Académica — SIGMA", FONT_SIGN_ROLE);
        slogan.setAlignment(Element.ALIGN_LEFT);

        textCell.addElement(univName);
        textCell.addElement(fac);
        textCell.addElement(prog);
        textCell.addElement(slogan);

        headerTable.addCell(textCell);
        doc.add(headerTable);
    }

    private void addCertificationBody(Document doc, StudentModality sm, DefenseExaminer examiner)
            throws DocumentException {

        User examinerUser = examiner.getExaminer();
        String examinerRole = translateExaminerType(examiner.getExaminerType());

        List<StudentModalityMember> members = sm.getMembers() != null ? sm.getMembers() : List.of();
        String studentNames;
        if (!members.isEmpty()) {
            studentNames = members.stream()
                    .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                    .collect(Collectors.joining(", "));
        } else {
            studentNames = sm.getLeader().getName() + " " + sm.getLeader().getLastName();
        }

        String defDateStr = sm.getDefenseDate() != null
                ? sm.getDefenseDate().format(DATETIME_FMT)
                : "la fecha registrada en el sistema";

        String bodyText =
            "El Programa Académico de " + sm.getProgramDegreeModality().getAcademicProgram().getName() +
            " de la Facultad de " + sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName() +
            " de la Universidad Surcolombiana, mediante el presente documento " +
            "CERTIFICA que el(la) docente y evaluador(a) Dra(o). " + examinerUser.getName() + " " + examinerUser.getLastName() +
            ", en su calidad de " + examinerRole +
            ", participó activamente en todas las etapas del proceso de evaluación de la modalidad de grado presentada por " +
            "el(los) estudiante(s) " + studentNames +
            ". Su participación incluyó la revisión exhaustiva de la documentación requerida, la asistencia a la sustentación " +
            "realizada el día " + defDateStr +
            " y el registro de su evaluación con criterios académicos rigurosos conforme a las normas " +
            "establecidas en el Acuerdo 071 de 2023 de la Universidad Surcolombiana.";

        Paragraph body = new Paragraph(bodyText, FONT_BODY);
        body.setAlignment(Element.ALIGN_JUSTIFIED);
        body.setLeading(0f, 1.4f);
        doc.add(body);
    }

    private void addExaminerDataTable(Document doc, DefenseExaminer examiner) throws DocumentException {
        User user = examiner.getExaminer();
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Nombre completo:", user.getName() + " " + user.getLastName(), alt = !alt);
        addDataRow(table, "Correo institucional:", user.getEmail(), alt = !alt);
        addDataRow(table, "Rol en la evaluación:", translateExaminerType(examiner.getExaminerType()), alt = !alt);
        addDataRow(table, "Fecha de asignación:", examiner.getAssignmentDate().format(DATE_FMT), alt = !alt);

        doc.add(table);
    }

    private void addModalityTable(Document doc, StudentModality sm) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Nombre de la modalidad:", sm.getProgramDegreeModality().getDegreeModality().getName(), alt = !alt);
        addDataRow(table, "Tipo de modalidad:", sm.getModalityType() != null ? translateModalityType(sm.getModalityType().name()) : "Individual", alt = !alt);

        List<StudentModalityMember> members = sm.getMembers() != null ? sm.getMembers() : List.of();
        String studentNames;
        if (!members.isEmpty()) {
            studentNames = members.stream()
                    .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                    .collect(Collectors.joining("; "));
        } else {
            studentNames = sm.getLeader().getName() + " " + sm.getLeader().getLastName();
        }
        addDataRow(table, "Estudiante(s):", studentNames, alt = !alt);

        addDataRow(table, "Director de proyecto:", sm.getProjectDirector() != null ? sm.getProjectDirector().getName() + " " + sm.getProjectDirector().getLastName() : "No asignado", alt = !alt);
        addDataRow(table, "Fecha de sustentación:", sm.getDefenseDate() != null ? sm.getDefenseDate().format(DATETIME_FMT) : "No registrada", alt = !alt);
        addDataRow(table, "Lugar de sustentación:", sm.getDefenseLocation() != null ? sm.getDefenseLocation() : "No registrado", alt = !alt);

        doc.add(table);
    }

    private void addParticipationTable(Document doc, StudentModality sm, DefenseExaminer examiner) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;

        // Etapas de participación
        String etapa1 = "✓ Asignación como " + translateExaminerType(examiner.getExaminerType());
        addDataRow(table, "Etapa I - Asignación:", etapa1, alt = !alt);

        String etapa2 = "✓ Revisión de documentación de propuesta y documentos finales";
        addDataRow(table, "Etapa II - Revisión:", etapa2, alt = !alt);

        String etapa3 = "✓ Asistencia y participación en sustentación";
        addDataRow(table, "Etapa III - Sustentación:", etapa3, alt = !alt);

        String etapa4 = "✓ Registro de evaluación y emisión de decisión";
        addDataRow(table, "Etapa IV - Evaluación:", etapa4, alt = !alt);

        // Decisión registrada
        Object statusObj = sm.getStatus();
        log.info("Estado de la modalidad en addParticipationTable - ID: {}, Status Object: {}, Status Class: {}", 
            sm.getId(), statusObj, statusObj != null ? statusObj.getClass().getSimpleName() : "null");
        
        String decisionText = translateModalityStatus(statusObj);
        log.info("Estado traducido: {}", decisionText);
        
        addDataRow(table, "Estado final de la modalidad:", decisionText, alt = !alt);

        doc.add(table);
    }

    private void addComplianceDeclaration(Document doc, DefenseExaminer examiner) throws DocumentException {
        Paragraph declaration = new Paragraph(
            "DECLARACIÓN DE CUMPLIMIENTO\n\n",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, INST_RED)
        );
        doc.add(declaration);

        String exRole = translateExaminerType(examiner.getExaminerType());
        String text = "El(la) abajo firmante, en su calidad de " + exRole +
            ", declara bajo su responsabilidad académica que:\n\n" +
            "1. Participé activamente en todas las etapas del proceso de evaluación de esta modalidad de grado.\n\n" +
            "2. Realicé la revisión exhaustiva de los documentos y materiales presentados por el(los) estudiante(s), " +
            "conforme a los criterios académicos y reglamentarios establecidos por la Universidad Surcolombiana.\n\n" +
            "3. Asistí a la sustentación oral de la modalidad en la fecha y lugar especificados en este documento.\n\n" +
            "4. Registré mi evaluación académica de manera rigurosa e imparcial, aplicando la rúbrica de criterios " +
            "institucionales.\n\n" +
            "5. Mi participación en este proceso ha sido registrada en el sistema académico institucional  " +
            "y forma parte de la trazabilidad académica requerida por la institución.\n\n" +
            "El presente acta certifica mi participación íntegra en el proceso de evaluación de la modalidad de grado, " +
            "constituyendo evidencia formal de cumplimiento de funciones académicas y aseguramiento de calidad.";

        Paragraph compliance = new Paragraph(text, FONT_BODY);
        compliance.setAlignment(Element.ALIGN_JUSTIFIED);
        compliance.setLeading(0f, 1.3f);
        doc.add(compliance);
    }

    private void addSignatureSection(Document doc, DefenseExaminer examiner) throws DocumentException {
        PdfPTable sigTable = new PdfPTable(1);
        sigTable.setWidthPercentage(60);
        sigTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigTable.setSpacingBefore(10f);

        String examinerName = examiner.getExaminer().getName() + " " + examiner.getExaminer().getLastName();
        String role = translateExaminerType(examiner.getExaminerType());

        PdfPCell sigCell = buildSignatureCell(examinerName, role);
        sigTable.addCell(sigCell);

        doc.add(sigTable);
    }

    private PdfPCell buildSignatureCell(String name, String role) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(10f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(10f);
        cell.setPaddingRight(10f);

        // Línea de firma
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(80);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setFixedHeight(22f);
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderWidthBottom(1f);
        lineCell.setBorderColorBottom(GRAY_DARK);
        lineTable.addCell(lineCell);
        cell.addElement(lineTable);

        Paragraph namePara = new Paragraph(name, FONT_SIGN_NAME);
        namePara.setAlignment(Element.ALIGN_CENTER);
        namePara.setSpacingBefore(4f);
        cell.addElement(namePara);

        Paragraph rolePara = new Paragraph(role, FONT_SIGN_ROLE);
        rolePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(rolePara);

        return cell;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void addDataRow(PdfPTable table, String label, String value, boolean alt) {
        BaseColor bg = alt ? ROW_BG_ALT : ROW_BG_LIGHT;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(bg);
        labelCell.setPadding(7f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", FONT_VALUE));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setBackgroundColor(bg);
        valueCell.setPadding(7f);
        table.addCell(valueCell);
    }

    private void addSectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4f);
        t.setSpacingAfter(0f);
        PdfPCell cell = new PdfPCell(new Phrase(title, FONT_SECTION_HDR));
        cell.setBackgroundColor(INST_RED);
        cell.setPadding(7f);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    private void addRedLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(3f);
        cell.setBackgroundColor(INST_RED);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    private void addGoldLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(2f);
        cell.setBackgroundColor(INST_GOLD);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    private void addSpacing(Document doc, float spacingPt) throws DocumentException {
        Paragraph sp = new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, spacingPt));
        sp.setLeading(spacingPt);
        doc.add(sp);
    }

    private void addFooter(Document doc, String certNumber) throws DocumentException {
        Paragraph footer = new Paragraph(
            "Este documento tiene validez oficial en el marco de los procesos académicos " +
            "de la Universidad Surcolombiana, conforme al Acuerdo 071 de 2023.",
            FONT_FOOTER
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingAfter(4f);
        doc.add(footer);

        Paragraph verif = new Paragraph("Código de verificación: " + certNumber, FONT_FOOTER);
        verif.setAlignment(Element.ALIGN_CENTER);
        doc.add(verif);
    }

    private String generateCertificateNumber(StudentModality studentModality, User examiner) {
        Long programId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();
        int year = LocalDateTime.now().getYear();
        int nextNumber = 1;
        while (true) {
            String candidate = String.format("ACTA-JUR-PROG%d-%d-%04d", programId, year, nextNumber);
            boolean exists = certificateRepository.findAll().stream()
                .anyMatch(cert -> cert.getCertificateNumber().equals(candidate));
            if (!exists) {
                return candidate;
            }
            nextNumber++;
        }
    }

    private String calculateFileHash(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error calculando hash del archivo: {}", e.getMessage());
            return "";
        }
    }

    private String translateExaminerType(ExaminerType type) {
        if (type == null) return "Jurado";
        return switch (type) {
            case PRIMARY_EXAMINER_1 -> "Jurado Principal 1";
            case PRIMARY_EXAMINER_2 -> "Jurado Principal 2";
            case TIEBREAKER_EXAMINER -> "Jurado de Desempate";
            default -> "Jurado";
        };
    }

    private String translateModalityType(String type) {
        if (type == null) return "Individual";
        return switch (type.toUpperCase()) {
            case "GROUP" -> "Grupal";
            case "INDIVIDUAL" -> "Individual";
            default -> type;
        };
    }

    private String translateModalityStatus(Object status) {
        if (status == null) return "No registrado";
        
        // Si es un enum, obtener su nombre
        String key;
        if (status instanceof Enum<?>) {
            key = ((Enum<?>) status).name();
        } else {
            key = status.toString();
        }
        
        log.debug("Traduciendo estado de modalidad: {}", key);
        
        return switch (key) {
            case "MODALITY_SELECTED" -> "Modalidad seleccionada";
            case "UNDER_REVIEW_PROGRAM_HEAD" -> "En revisión por jefe de programa";
            case "CORRECTIONS_REQUESTED_PROGRAM_HEAD" -> "Correcciones solicitadas por jefe de programa";
            case "CORRECTIONS_SUBMITTED" -> "Correcciones enviadas";
            case "CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD" -> "Correcciones enviadas a jefe de programa";
            case "CORRECTIONS_SUBMITTED_TO_COMMITTEE" -> "Correcciones enviadas a comité";
            case "CORRECTIONS_SUBMITTED_TO_EXAMINERS" -> "Correcciones enviadas a jueces";
            case "CORRECTIONS_APPROVED" -> "Correcciones aprobadas";
            case "CORRECTIONS_REJECTED_FINAL" -> "Correcciones rechazadas (final)";
            case "READY_FOR_PROGRAM_CURRICULUM_COMMITTEE" -> "Listo para comité de currículo";
            case "UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE" -> "En revisión por comité de currículo";
            case "CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE" -> "Correcciones solicitadas por comité de currículo";
            case "READY_FOR_DIRECTOR_ASSIGNMENT" -> "Listo para asignación de director";
            case "READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE" -> "Listo para aprobación por comité de currículo";
            case "APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE" -> "Aprobado por comité de currículo";
            case "PROPOSAL_APPROVED" -> "Propuesta aprobada";
            case "PENDING_PROGRAM_HEAD_FINAL_REVIEW" -> "Pendiente revisión final de jefatura";
            case "APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW" -> "Aprobado por jefatura (revisión final)";
            case "DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR" -> "Sustentación solicitada por director de proyecto";
            case "DEFENSE_SCHEDULED" -> "Sustentación programada";
            case "EXAMINERS_ASSIGNED" -> "Jueces asignados";
            case "READY_FOR_EXAMINERS" -> "Listo para jueces";
            case "DOCUMENTS_APPROVED_BY_EXAMINERS" -> "Documentos aprobados por jueces";
            case "SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS" -> "Documentos secundarios aprobados por jueces";
            case "DOCUMENT_REVIEW_TIEBREAKER_REQUIRED" -> "Requiere jurado de desempate";
            case "EDIT_REQUESTED_BY_STUDENT" -> "Edición solicitada por estudiante";
            case "CORRECTIONS_REQUESTED_EXAMINERS" -> "Correcciones solicitadas por jueces";
            case "READY_FOR_DEFENSE" -> "Listo para sustentación";
            case "FINAL_REVIEW_COMPLETED" -> "Revisión final completada";
            case "DEFENSE_COMPLETED" -> "Sustentación realizada";
            case "UNDER_EVALUATION_PRIMARY_EXAMINERS" -> "Evaluación por jueces principales";
            case "DISAGREEMENT_REQUIRES_TIEBREAKER" -> "Desacuerdo, requiere desempate";
            case "UNDER_EVALUATION_TIEBREAKER" -> "Evaluación por jurado de desempate";
            case "EVALUATION_COMPLETED" -> "Evaluación completada";
            case "PENDING_DISTINCTION_COMMITTEE_REVIEW" -> "Pendiente revisión de distinción por comité";
            case "GRADED_APPROVED" -> "Aprobado";
            case "GRADED_FAILED" -> "Reprobado";
            case "MODALITY_CLOSED" -> "Modalidad cerrada";
            case "SEMINAR_CANCELED" -> "Seminario cancelado";
            case "MODALITY_CANCELLED" -> "Modalidad cancelada";
            case "CANCELLATION_REQUESTED" -> "Cancelación solicitada";
            case "CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR" -> "Cancelación aprobada por director de proyecto";
            case "CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR" -> "Cancelación rechazada por director de proyecto";
            case "CANCELLED_WITHOUT_REPROVAL" -> "Cancelado sin reprobación";
            case "CANCELLATION_REJECTED" -> "Cancelación rechazada";
            case "CANCELLED_BY_CORRECTION_TIMEOUT" -> "Cancelado por tiempo de corrección";
            default -> key;
        };
    }
}

