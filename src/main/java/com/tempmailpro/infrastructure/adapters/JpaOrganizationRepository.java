package com.tempmailpro.infrastructure.adapters;

import com.tempmailpro.domain.models.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface JpaOrganizationRepository extends JpaRepository<Organization, Long> {

    // Cambiamos Template por Organization
    // Y usamos el nombre correcto para buscar dentro de la lista
    Optional<Organization> findByAllowedDomains(String domain);
}