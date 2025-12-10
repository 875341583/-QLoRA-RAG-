package adapter.controller;

import application.service.DynamicPathService;
import domain.entity.KnowledgeBase;
import domain.entity.NavigationPath;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Dynamic Path Update Controller
 * Handles venue layout change notifications and path update requests
 * 
 * @author Alex
 * @version 1.0
 */
@RestController
@RequestMapping("/api/dynamic-path")
public class DynamicPathController {
    
    private final DynamicPathService dynamicPathService;
    
    /**
     * Constructor injection of service dependency
     */
    public DynamicPathController(DynamicPathService dynamicPathService) {
        this.dynamicPathService = dynamicPathService;
    }
    
    /**
     * Handle venue layout change notification
     * Receives media file and initiates path update process with 10-minute timeout
     * 
     * @param mediaFile Media file for venue changes (JPEG, PNG, MP4 formats supported)
     * @return Update task ID for tracking progress
     */
    @PostMapping("/handle-layout-change")
    public ResponseEntity<PathUpdateResponse> handleLayoutChangeNotification(
            @RequestParam("mediaFile") @Valid @NotNull MultipartFile mediaFile) {
        try {
            String updateId = dynamicPathService.initiatePathUpdate(mediaFile);
            PathUpdateResponse response = new PathUpdateResponse(updateId, 
                "Path update initiated successfully. Use updateId to track progress.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new PathUpdateResponse(null, "Failed to initiate update: " + e.getMessage()));
        }
    }
    
    /**
     * Check supported media formats
     * 
     * @return List of supported media formats
     */
    @GetMapping("/supported-formats")
    public ResponseEntity<List<String>> getSupportedMediaFormats() {
        List<String> formats = dynamicPathService.getSupportedMediaFormats();
        return ResponseEntity.ok(formats);
    }
    
    /**
     * Record knowledge base change history
     * 
     * @param knowledgeBase Knowledge base change information
     * @return Recording result
     */
    @PostMapping("/record-change-history")
    public ResponseEntity<String> recordChangeHistory(
            @RequestBody @Valid KnowledgeBase knowledgeBase) {
        String result = dynamicPathService.recordChangeHistory(knowledgeBase);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Handle exceptions during update process
     * 
     * @param exceptionInfo Exception information DTO
     * @return Exception handling result
     */
    @PostMapping("/handle-exceptions")
    public ResponseEntity<String> handleUpdateExceptions(
            @RequestBody @Valid ExceptionInfoRequest exceptionInfo) {
        String result = dynamicPathService.handleUpdateExceptions(exceptionInfo);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get latest navigation path information
     * 
     * @param venueId Venue ID
     * @return Navigation path details
     */
    @GetMapping("/latest-path/{venueId}")
    public ResponseEntity<NavigationPath> getLatestNavigationPath(
            @PathVariable String venueId) {
        NavigationPath path = dynamicPathService.getLatestNavigationPath(venueId);
        return path != null ? ResponseEntity.ok(path) : ResponseEntity.notFound().build();
    }
    
    /**
     * Get path update status
     * 
     * @param updateId Update task ID
     * @return Update status information
     */
    @GetMapping("/update-status/{updateId}")
    public ResponseEntity<PathUpdateStatus> getUpdateStatus(@PathVariable String updateId) {
        PathUpdateStatus status = dynamicPathService.getUpdateStatus(updateId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Response DTO for path update initiation
     */
    public static class PathUpdateResponse {
        private final String updateId;
        private final String message;
        
        public PathUpdateResponse(String updateId, String message) {
            this.updateId = updateId;
            this.message = message;
        }
        
        public String getUpdateId() {
            return updateId;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Request DTO for exception information
     */
    public static class ExceptionInfoRequest {
        private String exceptionType;
        private String errorMessage;
        private String stackTrace;
        private String contextInfo;
        
        // Getters and setters
        public String getExceptionType() {
            return exceptionType;
        }
        
        public void setExceptionType(String exceptionType) {
            this.exceptionType = exceptionType;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getStackTrace() {
            return stackTrace;
        }
        
        public void setStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
        }
        
        public String getContextInfo() {
            return contextInfo;
        }
        
        public void setContextInfo(String contextInfo) {
            this.contextInfo = contextInfo;
        }
    }
    
    /**
     * DTO for path update status
     */
    public static class PathUpdateStatus {
        private final String updateId;
        private final String status; // PENDING, PROCESSING, COMPLETED, TIMEOUT, FAILED
        private final int progress; // 0-100
        private final String message;
        private final String estimatedCompletionTime;
        
        public PathUpdateStatus(String updateId, String status, int progress, 
                               String message, String estimatedCompletionTime) {
            this.updateId = updateId;
            this.status = status;
            this.progress = progress;
            this.message = message;
            this.estimatedCompletionTime = estimatedCompletionTime;
        }
        
        public String getUpdateId() {
            return updateId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public int getProgress() {
            return progress;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getEstimatedCompletionTime() {
            return estimatedCompletionTime;
        }
    }
}


// 内容由AI生成，仅供参考