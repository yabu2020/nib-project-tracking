package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.Api;
import com.nib.projecttracking.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiRepository extends JpaRepository<Api, Long> {
    
   
    List<Api> findByProject(Project project);
    
    
    List<Api> findByLifecycleStage(Api.ApiLifecycleStage stage);
    
   
    List<Api> findByStatus(Api.ApiStatus status);
    
 
    List<Api> findByOwner_Id(Long ownerId);
    
    
    @Query("SELECT a FROM Api a LEFT JOIN FETCH a.project LEFT JOIN FETCH a.owner WHERE a.lifecycleStage IN ?1")
    List<Api> findActiveDevelopmentApis(List<Api.ApiLifecycleStage> stages);
    
    
    @Query("SELECT a FROM Api a LEFT JOIN FETCH a.project LEFT JOIN FETCH a.owner WHERE a.lifecycleStage = 'PRODUCTION'")
    List<Api> findProductionApis();
    @Query("SELECT a FROM Api a WHERE a.project.id = :projectId")
List<Api> findByProjectId(@Param("projectId") Long projectId);
    
 
@Query("SELECT a FROM Api a LEFT JOIN FETCH a.project LEFT JOIN FETCH a.owner")
@Override
List<Api> findAll();
}