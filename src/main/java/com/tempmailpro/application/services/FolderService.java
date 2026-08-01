package com.tempmailpro.application.services;

import com.tempmailpro.domain.exceptions.PlanLimitExceededException;
import com.tempmailpro.domain.models.*;
import com.tempmailpro.infrastructure.adapters.JpaFolderRepository;
import com.tempmailpro.infrastructure.adapters.JpaSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final JpaFolderRepository folderRepository;
    private final JpaSubscriptionRepository subscriptionRepository;

    public List<Folder> getFoldersForUser(User user) {
        return folderRepository.findVisibleFolders(
                user.getOrganization().getId(),
                user.getId()
        );
    }

    @Transactional
    public Folder createFolder(String name, User user, boolean isShared) {
        Organization organization = user.getOrganization();

        // 1. VALIDACIÓN: Obtener la suscripción activa de la organización
        Subscription subscription = subscriptionRepository.findByOrganizationId(organization.getId())
                .orElse(Subscription.builder()
                        .planType(PlanType.FREE) // Si por alguna razón no hay registro, asumimos FREE
                        .status(SubscriptionStatus.ACTIVE)
                        .build());

        // 2. VALIDACIÓN: Contar cuántas carpetas tiene actualmente el Tenant
        // Dentro de la validación por PlanType en createFolder:
        long currentFolderCount = folderRepository.countByOrganizationId(organization.getId());

        switch (subscription.getPlanType()) {
            case FREE:
                if (currentFolderCount >= 1) { // Cambiado de 3 a 1 carpeta max
                    throw new PlanLimitExceededException("El plan gratuito está limitado a 1 sola carpeta. Actualiza tu plan para almacenamiento ilimitado.");
                }
                if (isShared) {
                    throw new IllegalArgumentException("Las carpetas compartidas pertenecen a los planes corporativos B2B.");
                }
                break;

            case PREMIUM:
            case PREMIUM_PLUS:
                // Carpetas ilimitadas para Solopreneurs pero privadas.
                if (isShared) {
                    throw new IllegalArgumentException("Las carpetas compartidas requieren un plan de equipo B2B.");
                }
                break;

            case STARTER_TEAM:
            case GROWTH_TEAM:
            case SCALE_UP:
            case ENTERPRISE:
                // B2B: Carpetas e inserciones compartidas permitidas e ilimitadas
                break;
        }

        // 3. VALIDACIÓN EXTRA DE NEGOCIO: Evitar nombres vacíos o excesivamente largos
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la carpeta no puede estar vacío.");
        }

        if (name.length() > 50) {
            throw new IllegalArgumentException("El nombre de la carpeta no puede superar los 50 caracteres.");
        }

        // Construcción y guardado seguro de la entidad
        Folder folder = Folder.builder()
                .name(name.trim())
                .organization(organization)
                .createdBy(user)
                .isShared(isShared)
                .build();

        return folderRepository.save(folder);
    }
}