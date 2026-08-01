package com.tempmailpro.application.services;

import com.tempmailpro.application.dtos.AuthRequest;
import com.tempmailpro.application.dtos.RegisterRequest;
import com.tempmailpro.domain.models.*;
import com.tempmailpro.infrastructure.adapters.JpaUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.tempmailpro.application.dtos.AuthResponse;
import com.tempmailpro.infrastructure.security.jwt.JwtUtil;
import com.tempmailpro.infrastructure.adapters.JpaOrganizationRepository;
import com.tempmailpro.infrastructure.adapters.JpaSubscriptionRepository;


import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtUtil jwtUtil;
    private final JpaUserRepository userRepository;
    private final JpaOrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final PasswordEncoder passwordEncoder;
    private final JpaSubscriptionRepository subscriptionRepository;

    @Transactional // Importante para que si algo falla, no se cree la org a medias
    public User register(RegisterRequest request) {
        // 1. Validar si el email ya existe
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        // 2. Lógica de Organización Híbrida
        Organization organization;
        UserRole role;

        Optional<Organization> matchingOrg = organizationService.findMatchingOrganization(request.getEmail());

        if (matchingOrg.isPresent()) {
            // ESCENARIO B2B: Existe una organización con auto-join para este dominio
            organization = matchingOrg.get();

            // Verificar si hay cupos (Seats)
            if (!organizationService.hasAvailableSeats(organization)) {
                throw new RuntimeException("La organización ha alcanzado el límite de miembros. Contacta al administrador.");
            }

            role = UserRole.USER; // Entra como trabajador
        } else {
            // ESCENARIO B2C: No hay match, creamos su propio Workspace personal
            organization = createPersonalOrganization(request.getUsername());
            role = UserRole.ADMIN; // Es el dueño de su propio espacio
        }

        // 3. Crear y guardar el usuario
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .organization(organization)
                .role(role)
                .status(UserStatus.ACTIVE) // Asumo que tienes este Enum
                .plan(UserPlan.FREE)       // Por defecto
                .build();

        return userRepository.save(user);
    }

    private Organization createPersonalOrganization(String username) {
        // 1. Crear y guardar la organización del Workspace Personal
        Organization org = new Organization();
        org.setName(username + "'s Workspace");
        org.setCorporate(false);
        org.setSubscriptionTier("FREE");
        org.setMaxSeats(1);
        org.setAutoJoinEnabled(false);
        org.setActive(true);
        Organization savedOrg = organizationRepository.save(org);

        // 2. SOLUCIÓN: Crear y guardar la suscripción FREE inicial para que no falle el Dashboard
        Subscription initialSub = Subscription.builder()
                .organization(savedOrg)
                .planType(PlanType.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .maxUsers(1)
                .aiRequestsLimit(0)
                .aiRequestsUsed(0)
                .endsAt(null) // El plan gratuito no expira automáticamente
                .build();
        subscriptionRepository.save(initialSub);

        return savedOrg;
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // Generamos el token real
        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername());
    }
}