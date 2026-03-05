package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.ActivityLog;
import com.nib.projecttracking.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activity-logs")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;


@GetMapping
public ResponseEntity<?> getLogs(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    try {
        System.out.println("📊 Fetching activity logs...");
        
        Page<ActivityLog> logsPage = activityLogService.getFilteredLogs(
            userId, action, entityType, startDate, endDate, page, size
        );

     
        List<Map<String, Object>> logDTOs = logsPage.getContent().stream()
            .map(log -> {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("id", log.getId());
                logMap.put("timestamp", log.getTimestamp());
                logMap.put("action", log.getAction());
                logMap.put("entityType", log.getEntityType());
                logMap.put("entityId", log.getEntityId());
                logMap.put("details", log.getDetails());
                logMap.put("ipAddress", log.getIpAddress());
                
                
                if (log.getUser() != null) {
                    logMap.put("user", Map.of(
                        "id", log.getUser().getId(),
                        "username", log.getUser().getUsername(),
                        "fullName", log.getUser().getFullName()
                    ));
                } else {
                    logMap.put("user", null);
                    System.out.println("⚠️ Log ID " + log.getId() + " has NULL user!");
                }
                
                return logMap;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", logDTOs);
        response.put("totalElements", logsPage.getTotalElements());
        response.put("totalPages", logsPage.getTotalPages());
        response.put("number", logsPage.getNumber());
        response.put("size", logsPage.getSize());
        response.put("numberOfElements", logsPage.getNumberOfElements());

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        System.err.println("❌ Error fetching logs: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch activity logs"));
    }
}

    @GetMapping("/recent")
    public ResponseEntity<List<ActivityLog>> getRecentLogs(
            @RequestParam(defaultValue = "10") int limit) {

        try {
            List<ActivityLog> recentLogs = activityLogService.getRecentLogs(limit);
            System.out.println("Recent logs fetched: " + recentLogs.size());
            return ResponseEntity.ok(recentLogs);
        } catch (Exception e) {
            System.err.println("Error fetching recent logs: " + e.getMessage());
            return ResponseEntity.ok(List.of()); 
        }
    }

   
    @GetMapping("/export")
    public ResponseEntity<String> exportLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        try {
            LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

            List<ActivityLog> logs = activityLogService.getFilteredLogsForExport(
                    userId, action, entityType, start, end);

            String csv = generateCsv(logs);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "activity-logs-" + LocalDateTime.now().toString().replace(":", "-") + ".csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);

        } catch (Exception e) {
            System.err.println("❌ Error exporting logs: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error exporting logs: " + e.getMessage());
        }
    }

   
    private String generateCsv(List<ActivityLog> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,User,Action,Entity Type,Entity ID,Details,IP Address\n");

        for (ActivityLog log : logs) {
            csv.append(log.getTimestamp()).append(",");
            
            String userDisplay = (log.getUser() != null) 
                    ? (log.getUser().getFullName() != null ? log.getUser().getFullName() : log.getUser().getUsername())
                    : "Unknown";
            csv.append(escapeCsv(userDisplay)).append(",");
            csv.append(escapeCsv(log.getAction())).append(",");
            csv.append(escapeCsv(log.getEntityType())).append(",");
            csv.append(log.getEntityId() != null ? log.getEntityId() : "").append(",");
            csv.append(escapeCsv(log.getDetails())).append(",");
            csv.append(escapeCsv(log.getIpAddress())).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}