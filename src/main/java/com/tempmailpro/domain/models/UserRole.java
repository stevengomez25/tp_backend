package com.tempmailpro.domain.models;

public enum UserRole {
    SUPERADMIN, // Nosotros (Gestión global del SaaS)
    ADMIN,      // Comprador del plan corporativo (Manager)
    USER        // Trabajador o usuario B2C individual
}