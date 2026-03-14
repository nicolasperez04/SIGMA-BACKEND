package com.SIGMA.USCO.Users.controller;

import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.service.ModalityService;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.dto.StudentProfileRequest;
import com.SIGMA.USCO.Users.service.StudentService;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final ModalityService modalityService;
    private final DocumentService documentService;
    private final StudentDocumentRepository studentDocumentRepository;
    private final UserRepository userRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;


    @PostMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateStudentProfile(@RequestBody StudentProfileRequest request){
        return studentService.updateStudentProfile(request);
    }

    @PostMapping("/profile/from-academic-history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateStudentProfileFromAcademicHistory(
            @RequestParam("file") MultipartFile file
    ) {
        return studentService.updateStudentProfileFromAcademicHistory(file);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentInfo() {
        return studentService.getStudentProfile();
    }

    @GetMapping("/modality/current")
    public ResponseEntity<?> getCurrentStudentModality() {
        return modalityService.getCurrentStudentModality();
    }

    @GetMapping("/documents/{studentDocumentId}/history")
    public ResponseEntity<?> getDocumentHistory(@PathVariable Long studentDocumentId) {
        return documentService.getDocumentHistory(studentDocumentId);
    }

    @PostMapping("/{studentModalityId}/request-cancellation")
    public ResponseEntity<?> requestCancellation(@PathVariable Long studentModalityId) {
        return modalityService.requestCancellation(studentModalityId);
    }

    @GetMapping("/my-documents")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyDocuments() {
        return studentService.getMyDocuments();
    }

    @PostMapping("/cancellation-document/{studentModalityId}")
    public ResponseEntity<?> uploadCancellationDocument(@PathVariable Long studentModalityId, @RequestParam("file") MultipartFile file) {
        documentService.uploadCancellationDocument(studentModalityId, file);
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Documento de justificación cargado correctamente"
                )
        );
    }

    @GetMapping("/documents/{studentDocumentId}/view")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> viewMyDocument(@PathVariable Long studentDocumentId) {
        // Obtener el usuario actual autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        User currentUser = userOpt.get();

        // Buscar el documento
        Optional<StudentDocument> docOpt = studentDocumentRepository.findById(studentDocumentId);
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Documento no encontrado");
        }

        StudentDocument document = docOpt.get();

        // Verificar que el usuario sea un miembro activo de la modalidad
        Long studentModalityId = document.getStudentModality().getId();
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModalityId,
                currentUser.getId()
        );

        if (!isActiveMember) {
            return ResponseEntity.status(403).body("No tienes permiso para ver este documento");
        }

        // Leer el archivo desde el sistema de archivos
        try {
            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(404).body("Archivo no encontrado o no legible");
            }

            // Detectar tipo de contenido
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Retornar el archivo como blob
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al leer el archivo: " + e.getMessage());
        }
    }




}
