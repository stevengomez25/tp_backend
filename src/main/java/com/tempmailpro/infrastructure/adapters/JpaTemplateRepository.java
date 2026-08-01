package com.tempmailpro.infrastructure.adapters;

import com.tempmailpro.domain.models.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaTemplateRepository extends JpaRepository<Template, Long> {
    Optional<Template> findById(Long id); // Corregido el nombre del parámetro de 'userId' a 'id' para evitar confusiones

    // Este método ya funciona con tu mapeo de 'owner.organization.id'
    long countByOwnerOrganizationId(Long organizationId);

    // Busca plantillas filtrando por el ID de la organización del dueño
    List<Template> findByOwnerOrganizationId(Long organizationId);
}