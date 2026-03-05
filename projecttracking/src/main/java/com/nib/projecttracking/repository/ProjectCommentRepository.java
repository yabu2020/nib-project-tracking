package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {
    
    
    List<ProjectComment> findByProject(com.nib.projecttracking.entity.Project project);
    
    @Query("SELECT c FROM ProjectComment c WHERE c.project.id = :projectId")
    List<ProjectComment> findByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT c FROM ProjectComment c WHERE c.project.id = :projectId AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    List<ProjectComment> findByProjectIdAndParentCommentIsNull(@Param("projectId") Long projectId);
    
    @Query("SELECT c FROM ProjectComment c WHERE c.parentComment.id = :parentCommentId ORDER BY c.createdAt ASC")
    List<ProjectComment> findByParentCommentId(@Param("parentCommentId") Long parentCommentId);
    
    
    @Query("SELECT c FROM ProjectComment c LEFT JOIN FETCH c.replies WHERE c.project.id = :projectId AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    List<ProjectComment> findRootCommentsWithRepliesByProjectId(@Param("projectId") Long projectId);
    
    

    @Query("SELECT COUNT(c) FROM ProjectComment c WHERE c.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(c) FROM ProjectComment c WHERE c.project.id = :projectId AND c.parentComment IS NULL")
    long countRootCommentsByProjectId(@Param("projectId") Long projectId);
    
    @Query("DELETE FROM ProjectComment c WHERE c.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}