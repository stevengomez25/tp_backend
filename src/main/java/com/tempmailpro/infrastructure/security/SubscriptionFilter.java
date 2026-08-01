package com.tempmailpro.infrastructure.security;

import com.tempmailpro.domain.models.Subscription;
import com.tempmailpro.domain.models.SubscriptionStatus;
import com.tempmailpro.domain.models.User;
import com.tempmailpro.infrastructure.adapters.JpaSubscriptionRepository;
import com.tempmailpro.infrastructure.adapters.JpaUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SubscriptionFilter extends OncePerRequestFilter {

    private final JpaUserRepository userRepository;
    private final JpaSubscriptionRepository subscriptionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }


        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. FILTRADO SELECTIVO: Solo bloqueamos si intentan CREAR, EDITAR o BORRAR carpetas o plantillas
        // Los métodos GET se permiten siempre para que no pierdan acceso de lectura a sus datos históricos
        boolean isTargetEndpoint = path.startsWith("/api/templates") || path.contains("/api/folders");
        boolean isWriteOperation = "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);

        if (isTargetEndpoint && isWriteOperation) {

            // Obtenemos el usuario autenticado desde el contexto de Spring Security
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (principal instanceof String username) {
                User user = userRepository.findByUsername(username).orElse(null);

                if (user != null && user.getOrganization() != null) {
                    Long orgId = user.getOrganization().getId();

                    // Buscamos el estado de la suscripción de su organización
                    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElse(null);

                    // 2. VALIDACIÓN DE REGLAS DE NEGOCIO
                    if (sub == null ||
                            sub.getStatus() != SubscriptionStatus.ACTIVE ||
                            (sub.getEndsAt() != null && sub.getEndsAt().isBefore(LocalDateTime.now()))) {

                        // Si no está activa o ya expiró, bloqueamos la tubería (Pipeline HTTP)
                        sendSubscriptionError(response, "Suscripción inactiva o expirada. Por favor realice el pago en el módulo de facturación para continuar utilizando estos servicios.");
                        return; // Rompe el flujo, la petición no llega al Controller
                    }
                }
            }
        }

        // Si todo está en orden, continúa hacia el controlador
        filterChain.doFilter(request, response);
    }

    /**
     * Helper para armar una respuesta JSON limpia en caso de bloqueo
     */
    private void sendSubscriptionError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden
        response.setContentType("application/json;charset=UTF-8");

        String jsonPayload = String.format(
                "{\"error\": \"SubscriptionRequired\", \"message\": \"%s\", \"timestamp\": \"%s\"}",
                message, LocalDateTime.now()
        );

        response.getWriter().write(jsonPayload);
    }
}