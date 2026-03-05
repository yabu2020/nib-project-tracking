package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.ActivityLog;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.repository.ActivityLogRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    /**
     * Log an activity with explicit IP (preferred when available)
     */
  @Transactional
public void logAction(User user, String action, String entityType,
                      Long entityId, String details, String ipAddress) {
    try {
        if (user == null || action == null || entityType == null) {
            System.err.println("⚠️ Cannot log activity: missing required fields");
            return;
        }

        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details != null ? details : ""); 
        log.setIpAddress(ipAddress != null ? ipAddress : getClientIpAddress());
        log.setTimestamp(LocalDateTime.now());  

        activityLogRepository.save(log);

        System.out.println("📝 Logged: " + action +
                " by " + (user != null ? user.getUsername() : "anonymous") +
                " | IP: " + log.getIpAddress());
    } catch (Exception e) {
        System.err.println("❌ Failed to log activity: " + e.getMessage());
        e.printStackTrace();
    }
}

    /**
     * Log with auto-detected IP (most common usage)
     */
    @Transactional
    public void logAction(User user, String action, String entityType,
                          Long entityId, String details) {
        logAction(user, action, entityType, entityId, details, null);
    }

    /**
     * Safely get client IP from current request (handles proxies)
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");

                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    int comma = ip.indexOf(',');
                    if (comma > 0) ip = ip.substring(0, comma).trim();
                }

                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
                    ip = request.getHeader("Proxy-Client-IP");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
                    ip = request.getHeader("WL-Proxy-Client-IP");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
                    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
                    ip = request.getHeader("X-Real-IP");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
                    ip = request.getRemoteAddr();

                if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) ip = "127.0.0.1";

                return ip;
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    /**
     * Get filtered logs with pagination (matches controller signature)
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getFilteredLogs(
            Long userId, String action, String entityType,
            LocalDateTime start, LocalDateTime end,
            int page, int size) {

      Specification<ActivityLog> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    root.join("user", JoinType.LEFT);

    if (userId != null) {
        predicates.add(cb.equal(root.get("user").get("id"), userId));
    }
    if (action != null && !action.trim().isEmpty()) {
        predicates.add(cb.equal(root.get("action"), action.trim()));
    }
    if (entityType != null && !entityType.trim().isEmpty()) {
        predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
    }
    if (start != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), start));
    }
    if (end != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), end));
    }
    query.distinct(true);

    return cb.and(predicates.toArray(new Predicate[0]));
};

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return activityLogRepository.findAll(spec, pageable);
    }

    /**
     * Overload for LocalDate (used by controller)
     */
    public Page<ActivityLog> getFilteredLogs(
            Long userId, String action, String entityType,
            LocalDate startDate, LocalDate endDate,
            int page, int size) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        return getFilteredLogs(userId, action, entityType, start, end, page, size);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getFilteredLogsForExport(
            Long userId, String action, String entityType,
            LocalDateTime start, LocalDateTime end) {

        Specification<ActivityLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            root.fetch("user", JoinType.LEFT);

            if (userId != null) predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (action != null && !action.trim().isEmpty()) predicates.add(cb.equal(root.get("action"), action));
            if (entityType != null && !entityType.trim().isEmpty()) predicates.add(cb.equal(root.get("entityType"), entityType));
            if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), start));
            if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), end));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return activityLogRepository.findAll(spec, Sort.by("timestamp").descending());
    }

    public List<ActivityLog> getFilteredLogsForExport(
            Long userId, String action, String entityType,
            LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        return getFilteredLogsForExport(userId, action, entityType, start, end);
    }

    public List<ActivityLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("timestamp").descending());
        return activityLogRepository.findAll(pageable).getContent();
    }

    public long countLogs(Long userId, String action, String entityType,
                          LocalDateTime start, LocalDateTime end) {

        Specification<ActivityLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (action != null && !action.isEmpty()) predicates.add(cb.equal(root.get("action"), action));
            if (entityType != null && !entityType.isEmpty()) predicates.add(cb.equal(root.get("entityType"), entityType));
            if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), start));
            if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), end));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return activityLogRepository.count(spec);
    }

    public long countLogs(Long userId, String action, String entityType,
                          LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return countLogs(userId, action, entityType, start, end);
    }

    public Optional<ActivityLog> getLogById(Long id) {
        return activityLogRepository.findById(id);
    }

    public List<String> getDistinctActions() {
        return activityLogRepository.findDistinctActions();
    }

    public List<String> getDistinctEntityTypes() {
        return activityLogRepository.findDistinctEntityTypes();
    }
}