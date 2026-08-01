package com.tempmailpro.infrastructure.adapters;

import com.tempmailpro.domain.models.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaFolderRepository extends JpaRepository<Folder, Long> {

    // Este mapea directo a folder.organization.id para el validador del plan
    long countByOrganizationId(Long organizationId);

    // Mapeamos explícitamente con @Query para las estadísticas generales,
    // evitando que Spring Data intente buscar una propiedad 'owner' inexistente.
    @Query("SELECT COUNT(f) FROM Folder f WHERE f.organization.id = :organizationId")
    long countFoldersByOrganizationId(@Param("organizationId") Long organizationId);

    // Tu buscador de visibilidad multi-tenant
    @Query("SELECT f FROM Folder f WHERE f.organization.id = :organizationId " +
            "AND (f.isShared = true OR f.createdBy.id = :userId)")
    List<Folder> findVisibleFolders(@Param("organizationId") Long organizationId,
                                    @Param("userId") Long userId);
}