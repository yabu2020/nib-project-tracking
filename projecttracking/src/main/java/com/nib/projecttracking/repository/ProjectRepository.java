package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {


    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager LEFT JOIN FETCH p.initiatedBy ORDER BY p.createdAt DESC")
    @Override
    List<Project> findAll();

    
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager LEFT JOIN FETCH p.initiatedBy WHERE p.id = :id")
    @Override
    Optional<Project> findById(@Param("id") Long id);
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager LEFT JOIN FETCH p.initiatedBy WHERE p.id = :id")
    Optional<Project> findProjectWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager WHERE p.status = :status")
    List<Project> findByStatus(@Param("status") Project.ProjectStatus status);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager WHERE p.ragStatus = :ragStatus")
    List<Project> findByRagStatus(@Param("ragStatus") Project.RagStatus ragStatus);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager WHERE p.ragStatus IN :statuses ORDER BY p.ragStatus")
    List<Project> findCriticalProjects(@Param("statuses") List<Project.RagStatus> statuses);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager WHERE p.status IN :statuses")
    List<Project> findActiveProjects(@Param("statuses") List<Project.ProjectStatus> statuses);


    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager WHERE p.manager = :manager")
    List<Project> findByManager(@Param("manager") User manager);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.initiatedBy WHERE p.initiatedBy = :initiatedBy")
    List<Project> findByInitiatedBy(@Param("initiatedBy") User initiatedBy);

    @Query("SELECT DISTINCT p FROM Project p JOIN p.tasks t WHERE t.assignedTo.id = :userId")
    List<Project> findProjectsByUserHasTasks(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.manager WHERE p.projectType = :projectType")
    List<Project> findByProjectType(@Param("projectType") String projectType);

    

    long countByStatus(Project.ProjectStatus status);

    @Query("""
        SELECT DISTINCT p FROM Project p
        LEFT JOIN FETCH p.manager
        WHERE p.startDate IS NOT NULL
        AND p.endDate IS NOT NULL
        AND p.startDate <= :endDate
        AND p.endDate >= :startDate
        ORDER BY p.createdAt DESC
    """)
    List<Project> findProjectsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}