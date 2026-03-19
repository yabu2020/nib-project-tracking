
package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.service.ActivityLogService;  
import com.nib.projecttracking.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
    origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"},
    allowCredentials = "true",
    allowedHeaders = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final ActivityLogService activityLogService; 


    public AuthController(
            AuthenticationManager authenticationManager, 
            UserService userService,
            ActivityLogService activityLogService) {  
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.activityLogService = activityLogService;  
    }

    /**
     * Login endpoint - creates session AND logs activity
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Username and password are required"));
            }

            System.out.println("🔐 Login attempt for: " + username);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            System.out.println("✅ Session created: " + session.getId());

            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found after authentication"));

            
            try {
                activityLogService.logAction(
                    user,  
                    "LOGIN",
                    "User",
                    user.getId(),
                    "User logged in successfully from IP: " + getClientIpAddress(request),
                    getClientIpAddress(request)
                );
                System.out.println("📝 Login activity logged for: " + username);
            } catch (Exception logError) {
                
                System.err.println("⚠️ Failed to log login activity: " + logError.getMessage());
            }

            System.out.println("✅ Login successful for: " + user.getUsername());

            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("fullName", user.getFullName());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole() != null ? user.getRole().name() : "USER");
            userInfo.put("mustResetPassword", user.getMustResetPassword() != null && user.getMustResetPassword());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Login successful");
            responseBody.put("user", userInfo);

            return ResponseEntity.ok(responseBody);

        } catch (BadCredentialsException e) {
            System.err.println("❌ Invalid credentials for: " + credentials.get("username"));
            

            try {
                
                userService.findByUsername(credentials.get("username")).ifPresent(user -> {
                    activityLogService.logAction(
                        user,
                        "LOGIN_FAILED",
                        "User",
                        user.getId(),
                        "Failed login attempt from IP: " + getClientIpAddress(request),
                        getClientIpAddress(request)
                    );
                });
            } catch (Exception logError) {
                System.err.println("⚠️ Failed to log failed login: " + logError.getMessage());
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid username or password"));

} catch (Exception e) {
    System.err.println("❌ Login error: " + e.getMessage());
    e.printStackTrace();
    
    if (e instanceof RuntimeException && e.getMessage().contains("User not found")) {
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "Invalid username or password"));
    }
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("success", false, "error", "Invalid username or password"));
}
    }

    /**
     * Logout - invalidates session AND logs activity
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            HttpServletRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdFromHeader) {  
        
        System.out.println("=== LOGOUT REQUEST RECEIVED ===");
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("X-User-Id header: " + userIdFromHeader);
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("Authentication: " + (auth != null ? auth.getName() : "null"));
            
            String username = auth != null ? auth.getName() : "unknown";
            User userToLog = null;

            if (userIdFromHeader != null) {
                System.out.println("📝 Using X-User-Id header: " + userIdFromHeader);
                userToLog = userService.findByUserId(userIdFromHeader).orElse(null);  
            }
            
           
            if (userToLog == null && auth != null && !"anonymousUser".equals(auth.getPrincipal())) {
                System.out.println("📝 Using SecurityContext username: " + username);
                userToLog = userService.findByUsername(username).orElse(null);
            }
            
         
            if (userToLog != null) {
                System.out.println("📝 Found user for logging: " + userToLog.getUsername() + " (ID: " + userToLog.getId() + ")");
                try {
                    activityLogService.logAction(
                        userToLog,
                        "LOGOUT",
                        "User",
                        userToLog.getId(),
                        "User logged out from IP: " + getClientIpAddress(request),
                        getClientIpAddress(request)
                    );
                    System.out.println("✅ Logout activity SAVED for: " + userToLog.getUsername());
                } catch (Exception logError) {
                    System.err.println("❌ Failed to save logout activity: " + logError.getMessage());
                    logError.printStackTrace();
                }
            } else {
                System.out.println("⚠️ No user found to log logout activity");
                System.out.println("  - userIdFromHeader: " + userIdFromHeader);
                System.out.println("  - auth principal: " + (auth != null ? auth.getPrincipal() : "null"));
            }

          
            SecurityContextHolder.clearContext();

          
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                System.out.println("🧹 Session invalidated: " + session.getId());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout successful"
            ));

        } catch (Exception e) {
            System.err.println("❌ Logout error: " + e.getMessage());
            e.printStackTrace();
           
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout successful"
            ));
        }
    }

    /**
     * Get current authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
           
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            HttpSession session = request.getSession(false);
            if (session != null) {
                String username = (String) session.getAttribute("user");
                if (username != null) {
                    User user = userService.findByUsername(username).orElse(null);
                    if (user != null) {
                        return ResponseEntity.ok(buildUserInfo(user));
                    }
                }
            }
            
            System.out.println("❌ /me called - No active session");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No active session"));
        }

        Object principal = auth.getPrincipal();
        User user = null;

        if (principal instanceof User) {
            user = (User) principal;
        }
        else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            user = userService.findByUsername(username).orElse(null);
        }

        if (user == null) {
            System.out.println("❌ /me - User not found in database");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
        }

        System.out.println("🔍 /me - Returning user: " + user.getUsername());
        return ResponseEntity.ok(buildUserInfo(user));
    }

    
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole() != null ? user.getRole().name() : "USER");
        userInfo.put("mustResetPassword", user.getMustResetPassword() != null && user.getMustResetPassword());
        return userInfo;
    }

   
    private String getClientIpAddress(HttpServletRequest request) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1";
            }
            return ip != null ? ip : "unknown";
        } catch (Exception e) {
            System.err.println("Error getting IP: " + e.getMessage());
            return "unknown";
        }
    }
}