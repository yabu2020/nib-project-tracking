package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    
    List<Task> findByProject(Project project);
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId")
List<Task> findByProjectId(@Param("projectId") Long projectId);
    
    List<Task> findByAssignedTo(User user);
    
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId")
    List<Task> findByAssignedToId(@Param("userId") Long userId);
    
    List<Task> findByStatus(Task.TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.dueDate < ?1 AND t.status != 'COMPLETED'")
    List<Task> findOverdueTasks(LocalDate date);
    
    @Query("SELECT t FROM Task t WHERE t.dueDate BETWEEN ?1 AND ?2")
    List<Task> findTasksDueThisWeek(LocalDate startDate, LocalDate endDate);

    long countByAssignedTo(User user);

    long countByAssignedToAndStatus(User user, Task.TaskStatus status);
    
   
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.project " +
           "LEFT JOIN FETCH t.assignedTo " +
           "LEFT JOIN FETCH t.assignedBy")
    List<Task> findAllWithRelationships();
    
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.project " +
           "LEFT JOIN FETCH t.assignedTo " +
           "LEFT JOIN FETCH t.assignedBy " +
           "WHERE t.assignedTo.id = :userId")
    List<Task> findByAssignedToIdWithRelationships(@Param("userId") Long userId);

@Query("SELECT DISTINCT t FROM Task t WHERE t.project.id = :projectId ORDER BY t.priority DESC, t.dueDate ASC")
List<Task> findTasksByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.project " +
           "LEFT JOIN FETCH t.assignedTo " +
           "LEFT JOIN FETCH t.assignedBy " +
           "WHERE t.project.id = :projectId")
    List<Task> findByProjectIdWithRelationships(@Param("projectId") Long projectId);
}