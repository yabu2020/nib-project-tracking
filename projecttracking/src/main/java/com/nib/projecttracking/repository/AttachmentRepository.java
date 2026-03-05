package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    
    
    @Query("SELECT a FROM Attachment a WHERE a.project.id = :projectId")
List<Attachment> findByProjectId(@Param("projectId") Long projectId);
    
    
    List<Attachment> findByTaskId(Long taskId);
    
   
    List<Attachment> findByUploadedById(Long userId);
    
    
    List<Attachment> findByProjectIdAndUploadedById(Long projectId, Long userId);
    
    
    long countByProjectId(Long projectId);
    
    
    long countByTaskId(Long taskId);
    
    
    
    @Query("SELECT a FROM Attachment a WHERE LOWER(a.originalFileName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Attachment> searchByFileName(@Param("keyword") String keyword);
}