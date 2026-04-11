package com.SIGMA.USCO.academic.controller;


import com.SIGMA.USCO.academic.dto.ProgramDTO;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.service.AcademicProgramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Programas Académicos", description = "Gestión de programas académicos: creación, actualización y consulta")
@RestController
@RequestMapping("/academic-programs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class AcademicProgramController {

    private final AcademicProgramService academicProgramService;

    @Operation(summary = "Crear programa académico", description = "Crea un nuevo programa académico en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Programa creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERM_CREATE_PROGRAM')")
    public ResponseEntity<?> createProgram(@RequestBody ProgramDTO request) {

        try {
            ProgramDTO program = academicProgramService.createProgram(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Programa académico creado exitosamente."
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Ocurrió un error al crear el programa académico.")
            );
        }
    }

    @Operation(summary = "Obtener programa por ID", description = "Retorna la información de un programa académico específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Programa obtenido"),
            @ApiResponse(responseCode = "404", description = "Programa no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getProgramById(@Parameter(description = "ID del programa") @PathVariable Long id) {
        try {
            ProgramDTO program = academicProgramService.getProgramById(id);
            return ResponseEntity.ok(program);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Ocurrió un error al obtener el programa académico.")
            );
        }
    }

    @Operation(summary = "Obtener todos los programas", description = "Retorna la lista completa de programas académicos")
    @ApiResponse(responseCode = "200", description = "Lista de programas obtenida")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('PERM_VIEW_PROGRAMS')")
    public ResponseEntity<?> getAllPrograms() {
        try {
            return ResponseEntity.ok(academicProgramService.getAllPrograms());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Ocurrió un error al obtener los programas académicos.")
            );
        }
    }

    @Operation(summary = "Obtener programas activos", description = "Retorna solo los programas académicos activos")
    @ApiResponse(responseCode = "200", description = "Lista de programas activos obtenida")
    @GetMapping("/active")
    public ResponseEntity<?> getActivePrograms() {
        try {
            return ResponseEntity.ok(academicProgramService.getActivePrograms());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Ocurrió un error al obtener los programas académicos activos.")
            );
        }
    }

    @Operation(summary = "Actualizar programa académico", description = "Actualiza la información de un programa académico existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Programa actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Programa no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PROGRAM')")
    public ResponseEntity<?> updateProgram(@Parameter(description = "ID del programa") @PathVariable Long id, @RequestBody ProgramDTO request) {
        try {
            AcademicProgram updatedProgram = academicProgramService.updateProgram(id, request);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Programa académico actualizado exitosamente."
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Ocurrió un error al actualizar el programa académico.")
            );
        }
    }
}
