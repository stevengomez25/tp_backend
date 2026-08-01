package com.tempmailpro.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "folders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Relación con la organización (Tenant)
    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Quién creó la carpeta (útil para carpetas privadas vs compartidas)
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    // Si es true, todos en la organización la ven. Si es false, solo el dueño.
    @Column(name = "is_shared")
    private Boolean isShared;

    // Forzamos a Jackson a usar el getter de Lombok para esta propiedad
    @JsonProperty("isShared")
    public Boolean getIsShared() {
        return this.isShared;
    }

    // Forzamos a Jackson a usar el setter de Lombok para esta propiedad
    @JsonProperty("isShared")
    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("folder")
    private List<Template> templates;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}