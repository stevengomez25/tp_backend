package com.tempmailpro.application.dtos; // O el paquete que uses para DTOs

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TemplateResponse {
    private Long id;
    private String name;
    private String subject;
    private String body;
    private Long folderId;
    private String folderName;
}