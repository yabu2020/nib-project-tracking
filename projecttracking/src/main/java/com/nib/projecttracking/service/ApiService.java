package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.Api;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.repository.ApiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApiService {

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ActivityLogService activityLogService;  
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    /**
     * Create new API (FR-02)
     */
    public Api createApi(Api api, Project project, User owner) {
        System.out.println("=== API SERVICE ===");
        System.out.println("Creating API: " + api.getApiName());

        api.setProject(project);
        api.setOwner(owner);
        api.setLifecycleStage(Api.ApiLifecycleStage.DOCUMENTATION);
        api.setStatus(Api.ApiStatus.ACTIVE);

        Api savedApi = apiRepository.save(api);
        System.out.println("API saved with ID: " + savedApi.getId());
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = String.format("Created API '%s' (v%s) in project '%s'",
                    savedApi.getApiName(), savedApi.getApiVersion(), project.getProjectName());
            activityLogService.logAction(
                    currentUser,
                    "API_CREATED",
                    "Api",
                    savedApi.getId(),
                    details
            );
        }

        return savedApi;
    }

    /**
     * Find API by ID
     */
    @Transactional(readOnly = true)
    public Optional<Api> findApiById(Long apiId) {
        return apiRepository.findById(apiId);
    }

    /**
     * Find all APIs
     */
    @Transactional(readOnly = true)
    public List<Api> findAllApis() {
        return apiRepository.findAll();
    }

    /**
     * Find APIs by project
     */
    @Transactional(readOnly = true)
    public List<Api> findApisByProject(Project project) {
        return apiRepository.findByProject(project);
    }

    /**
     * Find APIs by lifecycle stage
     */
    @Transactional(readOnly = true)
    public List<Api> findApisByStage(Api.ApiLifecycleStage stage) {
        return apiRepository.findByLifecycleStage(stage);
    }

    /**
     * Count APIs by stage
     */
    @Transactional(readOnly = true)
    public long countApisByStage(Api.ApiLifecycleStage stage) {
        return apiRepository.findByLifecycleStage(stage).size();
    }

    /**
     * Find APIs in active development (not yet in production)
     */
    @Transactional(readOnly = true)
    public List<Api> findActiveDevelopmentApis() {
        return apiRepository.findAll().stream()
                .filter(api -> api.getLifecycleStage() != null &&
                        api.getLifecycleStage() != Api.ApiLifecycleStage.PRODUCTION)
                .collect(Collectors.toList());
    }

    /**
     * Count APIs in development - FOR DASHBOARD
     */
    @Transactional(readOnly = true)
    public long countActiveDevelopmentApis() {
        return findActiveDevelopmentApis().size();
    }

    /**
     * Find production APIs
     */
    @Transactional(readOnly = true)
    public List<Api> findProductionApis() {
        return apiRepository.findAll().stream()
                .filter(api -> api.getLifecycleStage() == Api.ApiLifecycleStage.PRODUCTION)
                .collect(Collectors.toList());
    }

    /**
     * Count production APIs - FOR DASHBOARD
     */
    @Transactional(readOnly = true)
    public long countProductionApis() {
        return findProductionApis().size();
    }

    /**
     * Update API lifecycle stage
     */
    public Api updateApiLifecycleStage(Long apiId, Api.ApiLifecycleStage newStage) {
        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found"));

        Api.ApiLifecycleStage oldStage = api.getLifecycleStage();
        api.setLifecycleStage(newStage);
        if (newStage == Api.ApiLifecycleStage.DEVELOPMENT && api.getDevelopmentStartDate() == null) {
            api.setDevelopmentStartDate(LocalDate.now());
        }
        if (newStage == Api.ApiLifecycleStage.TESTING && api.getTestingStartDate() == null) {
            api.setTestingStartDate(LocalDate.now());
        }
        if (newStage == Api.ApiLifecycleStage.PRODUCTION && api.getProductionDate() == null) {
            api.setProductionDate(LocalDate.now());
        }

        Api updated = apiRepository.save(api);
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = String.format("Changed lifecycle stage from %s → %s for API '%s'",
                    oldStage, newStage, updated.getApiName());
            activityLogService.logAction(
                    currentUser,
                    "API_LIFECYCLE_UPDATED",
                    "Api",
                    updated.getId(),
                    details
            );
        }

        return updated;
    }

    /**
     * Update API status
     */
    public Api updateApiStatus(Long apiId, Api.ApiStatus newStatus) {
        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found"));

        Api.ApiStatus oldStatus = api.getStatus();
        api.setStatus(newStatus);

        Api updated = apiRepository.save(api);
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = String.format("Changed status from %s → %s for API '%s'",
                    oldStatus, newStatus, updated.getApiName());
            activityLogService.logAction(
                    currentUser,
                    "API_STATUS_UPDATED",
                    "Api",
                    updated.getId(),
                    details
            );
        }

        return updated;
    }

    /**
     * Update API details (name, version, description, endpoint, docs)
     */
    public Api updateApi(Long apiId, Api apiDetails) {
        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found"));

        api.setApiName(apiDetails.getApiName());
        api.setApiVersion(apiDetails.getApiVersion());
        api.setDescription(apiDetails.getDescription());
        api.setEndpoint(apiDetails.getEndpoint());
        api.setDocumentationUrl(apiDetails.getDocumentationUrl());

        Api updated = apiRepository.save(api);
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = String.format("Updated details of API '%s' (v%s)",
                    updated.getApiName(), updated.getApiVersion());
            activityLogService.logAction(
                    currentUser,
                    "API_UPDATED",
                    "Api",
                    updated.getId(),
                    details
            );
        }

        return updated;
    }

    /**
     * Delete API
     */
    public void deleteApi(Long apiId) {
        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found"));

        String apiName = api.getApiName();

        apiRepository.deleteById(apiId);
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = String.format("Deleted API '%s'", apiName);
            activityLogService.logAction(
                    currentUser,
                    "API_DELETED",
                    "Api",
                    apiId,
                    details
            );
        }
    }
}