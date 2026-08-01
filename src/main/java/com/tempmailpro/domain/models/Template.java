package com.tempmailpro.domain.models; // Esto DEBE coincidir con la carpeta

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "FK_TEMPLATE_FOLDER"))
    @JsonIgnoreProperties("templates") // Evita recursión si Angular lee la plantilla directamente
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // Evita que la plantilla intente serializar los datos completos de su dueño
    private User owner;

}