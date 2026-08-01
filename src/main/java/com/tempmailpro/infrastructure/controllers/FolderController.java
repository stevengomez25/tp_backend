package com.tempmailpro.infrastructure.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tempmailpro.application.services.FolderService;
import com.tempmailpro.domain.models.Folder;
import com.tempmailpro.domain.models.User;
import com.tempmailpro.infrastructure.adapters.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final JpaUserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Folder>> getUserFolders() {
        // Obtenemos el username del token JWT procesado por el JwtFilter
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(folderService.getFoldersForUser(user));
    }

    @PostMapping
    public ResponseEntity<Folder> createFolder(@RequestBody FolderRequest request) {
        System.out.println("VALOR RECIBIDO EN REQUEST: " + request.isShared());
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username).get();

        return ResponseEntity.ok(folderService.createFolder(request.getName(), user, request.isShared()));
    }
}

// DTO rápido para la petición
@lombok.Data
class FolderRequest {
    private String name;
    private boolean isShared;

    @JsonProperty("isShared")
    public boolean isShared() {
        return isShared;
    }

    @JsonProperty("isShared")
    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }
}