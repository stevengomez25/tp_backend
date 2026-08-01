package com.tempmailpro.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "organizations")
@Getter @Setter
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_corporate")
    private boolean isCorporate;

    private String domain;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "organization")
    @JsonIgnore // Evita que al serializar la Org se disparen en bucle sus miembros
    private List<User> members;

    @ElementCollection
    @CollectionTable(name = "organization_allowed_domains", joinColumns = @JoinColumn(name = "organization_id"))
    @Column(name = "domain")
    private List<String> allowedDomains;

    @Column(name = "auto_join_enabled")
    private boolean autoJoinEnabled = false;

    @Column(name = "subscription_tier")
    private String subscriptionTier;

    @Column(name = "max_seats")
    private int maxSeats;

    @Column(name = "wompi_customer_id")
    private String wompiCustomerId;

    @Column(name = "is_active")
    private boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}