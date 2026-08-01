package com.tempmailpro.application.services;

import com.tempmailpro.application.dtos.TemplateRequest;
import com.tempmailpro.domain.exceptions.PlanLimitExceededException;
import com.tempmailpro.domain.models.*;
import com.tempmailpro.infrastructure.adapters.JpaFolderRepository;
import com.tempmailpro.infrastructure.adapters.JpaSubscriptionRepository;
import com.tempmailpro.infrastructure.adapters.JpaTemplateRepository;
import com.tempmailpro.infrastructure.adapters.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final JpaTemplateRepository templateRepository;
    private final JpaUserRepository userRepository;
    private final JpaFolderRepository folderRepository;
    private final JpaSubscriptionRepository subscriptionRepository;

    // EL MÉTODO QUE FALTABA
    public List<Template> getTemplatesForUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Asumiendo que en tu JpaTemplateRepository tienes findByOwner(User owner)
        // o findByOwnerId. Ajusta según tu JpaTemplateRepository:
        return templateRepository.findByOwnerOrganizationId(user.getOrganization().getId());
    }

    @Transactional
    public Template createTemplate(TemplateRequest request) {
        String username = request.getOwnerUsername();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Organization organization = user.getOrganization();

        Subscription subscription = subscriptionRepository.findByOrganizationId(organization.getId())
                .orElse(Subscription.builder()
                        .planType(PlanType.FREE)
                        .status(SubscriptionStatus.ACTIVE)
                        .build());

        long currentTemplateCount = templateRepository.countByOwnerOrganizationId(organization.getId());

        switch (subscription.getPlanType()) {
            case FREE:
                if (currentTemplateCount >= 15) {
                    throw new PlanLimitExceededException("Has alcanzado el límite de 15 plantillas del plan gratuito.");
                }
                break;
            default:
                break;
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la plantilla es obligatorio.");
        }
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("El asunto del correo no puede estar vacío.");
        }

        Folder folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new RuntimeException("La carpeta especificada no existe."));

            if (!folder.getOrganization().getId().equals(organization.getId())) {
                throw new SecurityException("Acceso denegado: No puedes guardar una plantilla en una carpeta de otra organización.");
            }
        }

        Template template = new Template();
        template.setName(request.getTitle().trim());
        template.setSubject(request.getSubject().trim());
        template.setBody(request.getContent() != null ? request.getContent() : "");
        template.setOwner(user);
        template.setFolder(folder);

        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(Long id){
        // 1. Obtener el usuario actual (quién está haciendo la petición)
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Buscar la plantilla por su ID
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("La plantilla especificada no existe."));

        // 3. ¡SEGURIDAD CRÍTICA! Validar que la plantilla pertenezca a la organización del usuario
        Long currentUserOrgId = currentUser.getOrganization().getId();
        Long templateOrgId = template.getOwner().getOrganization().getId();

        if (!currentUserOrgId.equals(templateOrgId)) {
            // Si un usuario intenta borrar un ID que no es de su empresa, bloqueamos la acción
            throw new SecurityException("Acceso denegado: No tienes permiso para eliminar esta plantilla.");
        }

        // 4. Ejecutar la eliminación (Hard Delete usando el método nativo de JpaRepository)
        templateRepository.delete(template);
    }

    public Object getTemplateStats() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Long organizationId = user.getOrganization().getId();

        long totalFolders = folderRepository.countFoldersByOrganizationId(organizationId);
        long totalTemplates = templateRepository.countByOwnerOrganizationId(organizationId);

        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new RuntimeException("Suscripción no encontrada"));

        long maxSlotsPermitted = subscription.getMaxUsers();

        double slotsUsagePercentage = 0.0;
        if (maxSlotsPermitted > 0) {
            slotsUsagePercentage = ((double) totalTemplates / maxSlotsPermitted) * 100;
            slotsUsagePercentage = Math.round(slotsUsagePercentage * 100.0) / 100.0;
        }

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalTemplates", totalTemplates);
        stats.put("totalFolders", totalFolders);
        stats.put("slotsUsagePercentage", slotsUsagePercentage);

        return stats;
    }
    public Template getTemplateById(Long id) {
        // Busca en la base de datos y si alguien intenta buscar un ID fantasma, revienta controladamente
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
    }
}