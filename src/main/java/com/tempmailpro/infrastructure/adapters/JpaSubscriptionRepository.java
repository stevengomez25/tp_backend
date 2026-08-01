package com.tempmailpro.infrastructure.adapters;

import com.tempmailpro.domain.models.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaSubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByOrganizationId(Long organizationId);
}