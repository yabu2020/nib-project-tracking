package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByActiveTrue();
    
    List<User> findByActiveFalse();
    
    List<User> findByRole(User.Role role);
    
    List<User> findByDepartment(String department);
    
    @Query("SELECT u FROM User u WHERE u.role IN :roles")
    List<User> findManagers(@Param("roles") List<User.Role> roles);
    
    @Query("SELECT u FROM User u WHERE u.role IN :roles")
    List<User> findTechnicalStaff(@Param("roles") List<User.Role> roles);
   

@Query("SELECT DISTINCT u FROM User u " +
       "WHERE u.id IN (" +
       "  SELECT p.manager.id FROM Project p WHERE p.id = :projectId " +
       "  UNION " +
       "  SELECT p.initiatedBy.id FROM Project p WHERE p.id = :projectId " +
       "  UNION " +
       "  SELECT t.assignedTo.id FROM Task t WHERE t.project.id = :projectId " +
       ") " +
       "AND u.id != :excludeUserId")
List<User> findUsersByProjectId(@Param("projectId") Long projectId, @Param("excludeUserId") Long excludeUserId);
}