package com.tempmailpro.infrastructure.controllers;

import com.tempmailpro.application.dtos.TemplateRequest;
import com.tempmailpro.application.dtos.TemplateResponse;
import com.tempmailpro.application.services.TemplateService;
import com.tempmailpro.domain.models.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@RequestBody TemplateRequest request) {
        // Guardamos y mapeamos inmediatamente a DTO para evitar recursión cíclica
        Template createdTemplate = templateService.createTemplate(request);
        return ResponseEntity.ok(convertToResponseDTO(createdTemplate));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getUserTemplates() {
        List<Template> templates = templateService.getTemplatesForUser();

        List<TemplateResponse> response = templates.stream()
                .map(this::convertToResponseDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // Le pasamos el ID extraído de la URL al servicio
        templateService.deleteTemplate(id);

        // Devolvemos un código HTTP 204 (No Content) indicando que
        // se borró exitosamente y no hay nada más que devolver.
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(templateService.getTemplateStats());
    }

    // Helper method para reutilizar el mapeo limpio a DTO
    private TemplateResponse convertToResponseDTO(Template template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .subject(template.getSubject())
                .body(template.getBody())
                .folderId(template.getFolder() != null ? template.getFolder().getId() : null)
                .folderName(template.getFolder() != null ? template.getFolder().getName() : null)
                .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getById(@PathVariable Long id) {
        // 1. Buscamos la entidad original en la base de datos a través del servicio
        Template template = templateService.getTemplateById(id);

        // 2. Usamos tu propio helper para convertirla al DTO y la devolvemos con un 200 OK
        return ResponseEntity.ok(convertToResponseDTO(template));
    }
}