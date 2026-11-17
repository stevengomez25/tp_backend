package com.tempmailpro.application.dtos;

import lombok.Data;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;

@Data
public class TemplateRequest {
    private String title;
    private String subject;
    private String content;
    private String lang;

    @Nullable
    private Long folderId;
    
    public Long getFolderId() {
        return folderId;
    }

    public String getOwnerUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
        return null;
    }
}