package com.tempmailpro.application.dtos;

import lombok.Data;
import org.springframework.security.core.context.SecurityContextHolder;

@Data
public class TemplateRequest {
    private String title;
    private String subject;
    private String content;
    private String lang;
    private Long folderId;

    public String getOwnerUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
        return null;
    }
}