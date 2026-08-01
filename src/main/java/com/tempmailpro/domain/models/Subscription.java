package com.tempmailpro.domain.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle; // MONTHLY o YEARLY

    @Column(name = "max_users")
    private Integer maxUsers; // Límite de asientos según el Tier B2B (ej: 10 para Starter, 25 para Growth)

    @Column(name = "ai_requests_limit")
    private Integer aiRequestsLimit; // Pool de tokens de IA (ej: 100 para Premium+)

    @Column(name = "ai_requests_used")
    private Integer aiRequestsUsed;
}