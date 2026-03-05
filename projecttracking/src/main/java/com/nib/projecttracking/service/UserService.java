package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ActivityLogService activityLogService;

    private User getCurrentUser() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                System.out.println("⚠️ SecurityContext authentication is null");
                return null;
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            }
            System.out.println("⚠️ Principal is not a User instance: " + 
                (principal != null ? principal.getClass().getName() : "null"));
        } catch (Exception e) {
            System.err.println("❌ Error getting user from SecurityContext: " + e.getMessage());
        }
        return null;
    }
    
    private User getCurrentUser(Long fallbackUserId) {
     
        User user = getCurrentUser();
        if (user != null) {
            System.out.println("✅ Found actor via SecurityContext: " + user.getUsername());
            return user;
        }
        
        if (fallbackUserId != null) {
            System.out.println("⚠️ SecurityContext failed, trying fallback userId: " + fallbackUserId);
            User fallbackUser = userRepository.findById(fallbackUserId).orElse(null);
            if (fallbackUser != null) {
                System.out.println("✅ Found actor via fallback userId: " + fallbackUser.getUsername());
            } else {
                System.out.println("❌ Could not find user with fallback userId: " + fallbackUserId);
            }
            return fallbackUser;
        }
        
        System.out.println("❌ Could not determine actor for logging");
        return null;
    }

    public User createUser(User user) {
        return createUser(user, null);
    }
    public User createUser(User user, Long actorUserId) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        user.setMustResetPassword(true);

        User savedUser = userRepository.save(user);
        User actor = getCurrentUser(actorUserId);
        if (actor != null) {
            String details = String.format("Created new user: %s (%s) | Role: %s | Email: %s",
                    savedUser.getUsername(),
                    savedUser.getFullName() != null ? savedUser.getFullName() : "N/A",
                    savedUser.getRole(),
                    savedUser.getEmail());

            activityLogService.logAction(
                    actor,
                    "USER_CREATED",
                    "User",
                    savedUser.getId(),
                    details
            );
            System.out.println("📝 Logged USER_CREATED for user: " + savedUser.getUsername());
        } else {
            System.out.println("⚠️ USER_CREATED action NOT logged - could not identify actor");
        }

        return savedUser;
    }

      @Transactional(readOnly = true)
    public Optional<User> findByUserId(Long userId) {
        return userRepository.findById(userId);
    }
    

    @Transactional(readOnly = true)
    public Optional<User> findUserById(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> findAllActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<User> findAllInactiveUsers() {
        return userRepository.findByActiveFalse();
    }

    @Transactional(readOnly = true)
    public List<User> findAllManagers() {
        List<User.Role> managerRoles = List.of(
                User.Role.CEO,
                User.Role.DEPUTY_CHIEF,
                User.Role.DIRECTOR,
                User.Role.BUSINESS,
                User.Role.QUALITY_ASSURANCE,
                User.Role.CORE_BANKING_MANAGER,
                User.Role.DIGITAL_BANKING_MANAGER,
                User.Role.PROJECT_MANAGER
        );
        return userRepository.findManagers(managerRoles);
    }

    @Transactional(readOnly = true)
    public List<User> findAllTechnicalStaff() {
        List<User.Role> technicalRoles = List.of(
                User.Role.SENIOR_IT_OFFICER,
                User.Role.JUNIOR_IT_OFFICER,
                User.Role.IT_GRADUATE_TRAINEE,
                User.Role.DEVELOPER
        );
        return userRepository.findTechnicalStaff(technicalRoles);
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByDepartment(String department) {
        return userRepository.findByDepartment(department);
    }

    public User updateUser(Long userId, User userDetails) {
        return updateUser(userId, userDetails, null);
    }
    
    public User updateUser(Long userId, User userDetails, Long actorUserId) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        StringBuilder changes = new StringBuilder();

        if (userDetails.getEmail() != null && !userDetails.getEmail().isEmpty()) {
            if (!userDetails.getEmail().equals(existingUser.getEmail()) &&
                    userRepository.existsByEmail(userDetails.getEmail())) {
                throw new RuntimeException("Email already exists: " + userDetails.getEmail());
            }
            if (!userDetails.getEmail().equals(existingUser.getEmail())) {
                changes.append("Email changed; ");
            }
            existingUser.setEmail(userDetails.getEmail());
        }

        if (userDetails.getFullName() != null && !userDetails.getFullName().isEmpty() &&
                !userDetails.getFullName().equals(existingUser.getFullName())) {
            changes.append("Full name changed; ");
            existingUser.setFullName(userDetails.getFullName());
        }

       
        if (userDetails.getDepartment() != null &&
                !userDetails.getDepartment().equals(existingUser.getDepartment())) {
            changes.append("Department changed; ");
            existingUser.setDepartment(userDetails.getDepartment());
        }

        
        if (userDetails.getRole() != null &&
                !userDetails.getRole().equals(existingUser.getRole())) {
            changes.append("Role changed to ").append(userDetails.getRole()).append("; ");
            existingUser.setRole(userDetails.getRole());
        }

        if (userDetails.getMustResetPassword() != null &&
                !userDetails.getMustResetPassword().equals(existingUser.getMustResetPassword())) {
            changes.append("Must reset password flag changed; ");
            existingUser.setMustResetPassword(userDetails.getMustResetPassword());
        }

        if (userDetails.isActive() != existingUser.isActive()) {
            changes.append("Active status changed to ").append(userDetails.isActive() ? "active" : "inactive").append("; ");
            existingUser.setActive(userDetails.isActive());
        }

        existingUser.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(existingUser);

        if (changes.length() > 0) {
            User actor = getCurrentUser(actorUserId);
            if (actor != null) {
                String details = String.format("Updated user %s: %s",
                        updatedUser.getUsername(),
                        changes.toString().trim());

                activityLogService.logAction(
                        actor,
                        "USER_UPDATED",
                        "User",
                        userId,
                        details
                );
                System.out.println("📝 Logged USER_UPDATED for user: " + updatedUser.getUsername());
            } else {
                System.out.println("⚠️ USER_UPDATED action NOT logged - could not identify actor");
            }
        }

        return updatedUser;
    }

    public User updatePassword(Long userId, String newPassword) {
        return updatePassword(userId, newPassword, null);
    }
    
    public User updatePassword(Long userId, String newPassword, Long actorUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustResetPassword(false);
        user.setUpdatedAt(LocalDateTime.now());

        User updated = userRepository.save(user);

        User actor = getCurrentUser(actorUserId);
        if (actor != null) {
            String details = String.format("Password updated for user %s (%s)",
                    user.getUsername(),
                    user.getFullName() != null ? user.getFullName() : "N/A");

            activityLogService.logAction(
                    actor,
                    "PASSWORD_CHANGED",
                    "User",
                    userId,
                    details
            );
            System.out.println("📝 Logged PASSWORD_CHANGED for user: " + user.getUsername());
        } else {
            System.out.println("⚠️ PASSWORD_CHANGED action NOT logged - could not identify actor");
        }

        return updated;
    }

    public User resetUserPassword(Long userId, String newPassword) {
        return resetUserPassword(userId, newPassword, null);
    }
    
    public User resetUserPassword(Long userId, String newPassword, Long actorUserId) {
       
        return updatePassword(userId, newPassword, actorUserId);
    }

    public User deactivateUser(Long userId) {
        return deactivateUser(userId, null);
    }
    
    public User deactivateUser(Long userId, Long actorUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (!user.isActive()) {
            throw new RuntimeException("User is already deactivated");
        }

        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());

        User deactivated = userRepository.save(user);

        User actor = getCurrentUser(actorUserId);
        if (actor != null) {
            String details = String.format("Deactivated user %s (%s)",
                    user.getUsername(),
                    user.getFullName() != null ? user.getFullName() : "N/A");

            activityLogService.logAction(
                    actor,
                    "USER_DEACTIVATED",
                    "User",
                    userId,
                    details
            );
            System.out.println("📝 Logged USER_DEACTIVATED for user: " + user.getUsername());
        } else {
            System.out.println("⚠️ USER_DEACTIVATED action NOT logged - could not identify actor");
        }

        return deactivated;
    }

    public User reactivateUser(Long userId) {
        return reactivateUser(userId, null);
    }
    
    public User reactivateUser(Long userId, Long actorUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.isActive()) {
            throw new RuntimeException("User is already active");
        }

        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());

        User reactivated = userRepository.save(user);

        User actor = getCurrentUser(actorUserId);
        if (actor != null) {
            String details = String.format("Reactivated user %s (%s)",
                    user.getUsername(),
                    user.getFullName() != null ? user.getFullName() : "N/A");

            activityLogService.logAction(
                    actor,
                    "USER_REACTIVATED",
                    "User",
                    userId,
                    details
            );
            System.out.println("📝 Logged USER_REACTIVATED for user: " + user.getUsername());
        } else {
            System.out.println("⚠️ USER_REACTIVATED action NOT logged - could not identify actor");
        }

        return reactivated;
    }

   
    public void deleteUser(Long userId) {
        deleteUser(userId, null);
    }
    
   
    public void deleteUser(Long userId, Long actorUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String username = user.getUsername();
        String fullName = user.getFullName() != null ? user.getFullName() : "N/A";

        userRepository.deleteById(userId);

     
        User actor = getCurrentUser(actorUserId);
        if (actor != null) {
            String details = String.format("Permanently deleted user %s (%s)",
                    username, fullName);

            activityLogService.logAction(
                    actor,
                    "USER_DELETED",
                    "User",
                    userId,
                    details
            );
            System.out.println("📝 Logged USER_DELETED for user: " + username);
        } else {
            System.out.println("⚠️ USER_DELETED action NOT logged - could not identify actor");
        }
    }
}