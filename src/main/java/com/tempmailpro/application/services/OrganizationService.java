package com.tempmailpro.application.services;

import com.tempmailpro.domain.models.Organization;
import com.tempmailpro.domain.models.User;
import com.tempmailpro.infrastructure.adapters.JpaOrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final JpaOrganizationRepository organizationRepository;

    public Optional<Organization> findMatchingOrganization(String email) {
        String domain = email.substring(email.indexOf("@") + 1);

        // La consulta se hace directamente en SQL (mucho más rápido)
        return organizationRepository.findByAllowedDomains(domain)
                .filter(org -> org.isActive() && org.isAutoJoinEnabled());
    }

    public boolean hasAvailableSeats(Organization org) {
        // Tip: Si notas que org.getMembers() da error de LazyInitialization,
        // podrías necesitar un conteo directo en el repo, pero por ahora está bien.
        return org.getMembers().size() < org.getMaxSeats();
    }
}