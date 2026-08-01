package com.tempmailpro.infrastructure.controllers;

import com.tempmailpro.application.dtos.AuthRequest;
import com.tempmailpro.application.dtos.RegisterRequest;
import com.tempmailpro.application.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Ruta para crear cuenta: POST http://localhost:8080/api/auth/register
    @PostMapping({"/register","/signup"})
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // Ruta para iniciar sesión: POST http://localhost:8080/api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}