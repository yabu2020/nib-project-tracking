package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.Api;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apis")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ApiController {
    
    @Autowired
    private ApiService apiService;
    
    
@GetMapping
public ResponseEntity<List<Api>> getAllApis() {
    List<Api> apis = apiService.findAllApis();
    return ResponseEntity.ok(apis);
}
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getApiById(@PathVariable Long id) {
        return apiService.findApiById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
  
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Api>> getApisByProject(@PathVariable Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        return ResponseEntity.ok(apiService.findApisByProject(project));
    }
    
  
    @GetMapping("/stage/{stage}")
    public ResponseEntity<List<Api>> getApisByStage(@PathVariable Api.ApiLifecycleStage stage) {
        return ResponseEntity.ok(apiService.findApisByStage(stage));
    }
 
    @GetMapping("/production")
    public ResponseEntity<List<Api>> getProductionApis() {
        return ResponseEntity.ok(apiService.findProductionApis());
    }
  
    @GetMapping("/development")
    public ResponseEntity<List<Api>> getDevelopmentApis() {
        return ResponseEntity.ok(apiService.findActiveDevelopmentApis());
    }

    @PostMapping
    public ResponseEntity<?> createApi(@RequestBody Map<String, Object> apiData) {
        try {
            System.out.println("=== CREATE API REQUEST ===");
            System.out.println("Request data: " + apiData);
            
            Api api = new Api();
            api.setApiName((String) apiData.get("apiName"));
            api.setApiVersion((String) apiData.get("apiVersion"));
            api.setDescription((String) apiData.get("description"));
            api.setEndpoint((String) apiData.get("endpoint"));
            api.setDocumentationUrl((String) apiData.get("documentationUrl"));
            
            Project project = new Project();
            if (apiData.get("projectId") != null) {
                project.setId(Long.valueOf(apiData.get("projectId").toString()));
            }
            
            User owner = new User();
            if (apiData.get("ownerId") != null) {
                owner.setId(Long.valueOf(apiData.get("ownerId").toString()));
            }
            
            Api createdApi = apiService.createApi(api, project, owner);
            
            System.out.println("API created successfully with ID: " + createdApi.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "API created successfully",
                "api", createdApi
            ));
        } catch (Exception e) {
            System.err.println("Error creating API: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/stage")
    public ResponseEntity<?> updateApiStage(@PathVariable Long id, 
                                             @RequestBody Map<String, String> data) {
        try {
            Api.ApiLifecycleStage stage = Api.ApiLifecycleStage.valueOf(data.get("stage"));
            Api updatedApi = apiService.updateApiLifecycleStage(id, stage);
            return ResponseEntity.ok(Map.of(
                "message", "API stage updated successfully",
                "api", updatedApi
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
   
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateApiStatus(@PathVariable Long id, 
                                              @RequestBody Map<String, String> data) {
        try {
            Api.ApiStatus status = Api.ApiStatus.valueOf(data.get("status"));
            Api updatedApi = apiService.updateApiStatus(id, status);
            return ResponseEntity.ok(Map.of(
                "message", "API status updated successfully",
                "api", updatedApi
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateApi(@PathVariable Long id, 
                                        @RequestBody Map<String, Object> apiData) {
        try {
            Api apiDetails = new Api();
            apiDetails.setApiName((String) apiData.get("apiName"));
            apiDetails.setApiVersion((String) apiData.get("apiVersion"));
            apiDetails.setDescription((String) apiData.get("description"));
            apiDetails.setEndpoint((String) apiData.get("endpoint"));
            apiDetails.setDocumentationUrl((String) apiData.get("documentationUrl"));
            
            Api updatedApi = apiService.updateApi(id, apiDetails);
            return ResponseEntity.ok(Map.of(
                "message", "API updated successfully",
                "api", updatedApi
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApi(@PathVariable Long id) {
        try {
            apiService.deleteApi(id);
            return ResponseEntity.ok(Map.of("message", "API deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}