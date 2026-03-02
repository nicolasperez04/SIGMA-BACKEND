package com.SIGMA.USCO.documents.controller;

import com.SIGMA.USCO.documents.service.TemplateDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateDocumentController {

    private final TemplateDocumentService templateDocumentService;

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadTemplate(@PathVariable Long id) {
        return templateDocumentService.downloadTemplate(id);
    }

}

