package com.tempmailpro.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter // Recomiendo usar Getter/Setter explícitos en modelos JPA
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonIgnore // Jamás expongas el hash de la contraseña en los JSON
    private String password;

    @ManyToOne(fetch = FetchType.EAGER) // Mantenlo EAGER o LAZY según estructura de Auth
    @JoinColumn(name = "organization_id")
    private Organization organization; // Angular recupera el acceso aquí

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    private UserPlan plan;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Corta el bucle hacia atrás perfectamente
    private List<Template> templates;
}