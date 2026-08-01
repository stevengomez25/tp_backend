package com.tempmailpro.domain.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referencia única que le enviaremos a Wompi (ej: TX-123456)
    @Column(nullable = false, unique = true)
    private String reference;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Quién inició el pago

    @Column(nullable = false)
    private Long amountInCents; // Wompi maneja centavos (ej: $10.000 COP = 1000000)

    @Column(nullable = false)
    private String currency; // "COP"

    @Enumerated(EnumType.STRING)
    private PlanType targetPlan;

    @Column(nullable = false)
    private String status; // "PENDING", "APPROVED", "DECLINED", "ERROR"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}