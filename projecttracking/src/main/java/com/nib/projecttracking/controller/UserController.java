package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class UserController {
    
    @Autowired
    private UserService userService;
    
    
    private Long extractActorUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.valueOf(userIdHeader);
                System.out.println("🔍 Extracted X-User-Id header: " + userId);
                return userId;
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Invalid X-User-Id header format: " + userIdHeader);
            }
        }
        return null;
    }

    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.findUserById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/managers")
    public ResponseEntity<List<User>> getAllManagers() {
        return ResponseEntity.ok(userService.findAllManagers());
    }
    
    @GetMapping("/technical-staff")
    public ResponseEntity<List<User>> getAllTechnicalStaff() {
        return ResponseEntity.ok(userService.findAllTechnicalStaff());
    }
    
    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable User.Role role) {
        return ResponseEntity.ok(userService.findUsersByRole(role));
    }
    
    @GetMapping("/department/{department}")
    public ResponseEntity<List<User>> getUsersByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(userService.findUsersByDepartment(department));
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        return ResponseEntity.ok(userService.findAllActiveUsers());
    }
    
    @GetMapping("/inactive")
    public ResponseEntity<List<User>> getInactiveUsers() {
        return ResponseEntity.ok(userService.findAllInactiveUsers());
    }
    
    
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> userData, 
                                        HttpServletRequest request) {
        try {
            User user = new User();
            user.setUsername((String) userData.get("username"));
            user.setPassword((String) userData.get("password"));
            user.setFullName((String) userData.get("fullName"));
            user.setEmail((String) userData.get("email"));
            user.setDepartment((String) userData.get("department"));
            
            if (userData.get("role") != null) {
                user.setRole(User.Role.valueOf(userData.get("role").toString().toUpperCase()));
            }
            
            user.setMustResetPassword(true);
            
            Long actorUserId = extractActorUserId(request);
            User createdUser = userService.createUser(user, actorUserId);
            
            return ResponseEntity.ok(Map.of(
                "message", "User created successfully",
                "user", createdUser
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role specified"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create user"));
        }
    }
    
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                        @RequestBody Map<String, Object> userData,
                                        HttpServletRequest request) {
        try {
            User userDetails = new User();
            userDetails.setFullName((String) userData.get("fullName"));
            userDetails.setEmail((String) userData.get("email"));
            userDetails.setDepartment((String) userData.get("department"));
            userDetails.setActive((Boolean) userData.getOrDefault("active", true));
            
            if (userData.get("role") != null) {
                userDetails.setRole(User.Role.valueOf(userData.get("role").toString().toUpperCase()));
            }
            
            if (userData.get("mustResetPassword") != null) {
                userDetails.setMustResetPassword((Boolean) userData.get("mustResetPassword"));
            }
          
            Long actorUserId = extractActorUserId(request);
            User updatedUser = userService.updateUser(id, userDetails, actorUserId);
            
            return ResponseEntity.ok(Map.of(
                "message", "User updated successfully",
                "user", updatedUser
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role specified"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update user"));
        }
    }
    
    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Long id, 
                                            @RequestBody Map<String, String> data,
                                            HttpServletRequest request) {
        try {
            String newPassword = data.get("password");
            
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            }
            
            
            Long actorUserId = extractActorUserId(request);
            User updatedUser = userService.updatePassword(id, newPassword, actorUserId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully",
                "user", Map.of(
                    "id", updatedUser.getId(),
                    "username", updatedUser.getUsername(),
                    "mustResetPassword", updatedUser.getMustResetPassword()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, 
                                           @RequestBody Map<String, String> passwordData,
                                           HttpServletRequest request) {
        try {
            String newPassword = passwordData.get("newPassword");
            String confirmPassword = passwordData.get("confirmPassword");
            
            if (newPassword == null || confirmPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing password fields"));
            }
            
            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
            }
            
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            }
           
            Long actorUserId = extractActorUserId(request);
            User updatedUser = userService.resetUserPassword(id, newPassword, actorUserId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully",
                "user", updatedUser
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id, 
                                            HttpServletRequest request) {
        try {
           
            Long actorUserId = extractActorUserId(request);
            User deactivatedUser = userService.deactivateUser(id, actorUserId);
            
            return ResponseEntity.ok(Map.of(
                "message", "User deactivated successfully",
                "user", deactivatedUser
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id, 
                                            HttpServletRequest request) {
        try {
          
            Long actorUserId = extractActorUserId(request);
            User reactivatedUser = userService.reactivateUser(id, actorUserId);
            
            return ResponseEntity.ok(Map.of(
                "message", "User reactivated successfully",
                "user", reactivatedUser
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, 
                                        HttpServletRequest request) {
        try {
            Long actorUserId = extractActorUserId(request);
            userService.deleteUser(id, actorUserId);
            
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<User> allUsers = userService.findAllUsers();
            stats.put("totalUsers", allUsers.size());
            stats.put("activeUsers", userService.findAllActiveUsers().size());
            stats.put("inactiveUsers", userService.findAllInactiveUsers().size());
            
            Map<String, Long> usersByRole = new HashMap<>();
            for (User.Role role : User.Role.values()) {
                long count = userService.findUsersByRole(role).size();
                if (count > 0) {
                    usersByRole.put(role.name(), count);
                }
            }
            stats.put("usersByRole", usersByRole);
            
            Map<String, Long> usersByDept = new HashMap<>();
            for (User user : allUsers) {
                String dept = user.getDepartment() != null ? user.getDepartment() : "Unassigned";
                usersByDept.put(dept, usersByDept.getOrDefault(dept, 0L) + 1);
            }
            stats.put("usersByDepartment", usersByDept);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(stats);
    }
}