package com.tempmailpro.infrastructure.adapters;

import com.tempmailpro.domain.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaOrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByReference(String reference);
}