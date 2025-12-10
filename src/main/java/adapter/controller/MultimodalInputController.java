package adapter.controller;

import application.service.MultimodalInputService;
import application.service.RealTimeNavigationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Multimodal Input Processing Controller
 * Handles navigation requests initiated by users through images, text, and video
 * Supports concurrent requests from multiple users
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/multimodal-input")
public class MultimodalInputController {

    private final MultimodalInputService multimodalInputService;
    private final RealTimeNavigationService realTimeNavigationService;
    private final Executor asyncTaskExecutor;

    /**
     * Constructor injection of service dependencies
     *
     * @param multimodalInputService multimodal input processing service
     * @param realTimeNavigationService real-time navigation service
     * @param asyncTaskExecutor asynchronous task executor
     */
    @Autowired
    public MultimodalInputController(MultimodalInputService multimodalInputService,
                                   RealTimeNavigationService realTimeNavigationService,
                                   Executor asyncTaskExecutor) {
        this.multimodalInputService = multimodalInputService;
        this.realTimeNavigationService = realTimeNavigationService;
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    /**
     * Process image upload and parse location information
     *
     * @param imageFile user uploaded image file
     * @param userId user ID
     * @return navigation path result
     */
    @PostMapping("/image-upload")
    public ResponseEntity<String> handleImageUpload(
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam("userId") Long userId) {
        log.info("Received image upload request, user ID: {}", userId);
        
        try {
            if (imageFile == null || imageFile.isEmpty()) {
                log.warn("Image file is empty or null, user ID: {}", userId);
                return ResponseEntity.badRequest().body("Image file cannot be empty");
            }

            if (!isValidImageFormat(imageFile)) {
                log.warn("Unsupported image format, user ID: {}, content type: {}", 
                        userId, imageFile.getContentType());
                return ResponseEntity.badRequest()
                        .body("Unsupported file format. Only JPEG and PNG are supported");
            }

            log.debug("Processing image upload, file size: {} bytes, user ID: {}", 
                    imageFile.getSize(), userId);
            String navigationResult = multimodalInputService.parseImageContent(imageFile, userId);
            
            log.info("Image upload processing completed successfully, user ID: {}", userId);
            return ResponseEntity.ok(navigationResult);
        } catch (Exception e) {
            log.error("Image upload processing failed, user ID: {}, error: {}", 
                    userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Image processing failed: " + e.getMessage());
        }
    }

    /**
     * Process text description and identify target location
     *
     * @param textDescription user input text description
     * @param userId user ID
     * @return target location identification result
     */
    @PostMapping("/text-description")
    public ResponseEntity<String> processTextDescription(
            @RequestParam(value = "textDescription", required = false) String textDescription,
            @RequestParam("userId") Long userId) {
        log.info("Received text description request, user ID: {}", userId);
        
        try {
            if (textDescription == null || textDescription.trim().isEmpty()) {
                log.warn("Text description is empty, user ID: {}", userId);
                return ResponseEntity.badRequest().body("Text description cannot be empty");
            }

            log.debug("Processing text description, length: {}, user ID: {}", 
                    textDescription.length(), userId);
            String locationResult = multimodalInputService.extractTextVideoFeatures(textDescription, userId);
            
            log.info("Text description processing completed successfully, user ID: {}", userId);
            return ResponseEntity.ok(locationResult);
        } catch (Exception e) {
            log.error("Text description processing failed, user ID: {}, error: {}", 
                    userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Text processing failed: " + e.getMessage());
        }
    }

    /**
     * Start real-time video stream continuous navigation
     *
     * @param videoStream real-time video stream data
     * @param userId user ID
     * @param deviceInfo device information
     * @return real-time navigation session ID
     */
    @PostMapping("/video-navigation")
    public ResponseEntity<String> startVideoStreamNavigation(
            @RequestParam(value = "videoStream", required = false) MultipartFile videoStream,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "deviceInfo", required = false) String deviceInfo) {
        log.info("Received video stream navigation request, user ID: {}, device info: {}", 
                userId, deviceInfo);
        
        try {
            if (videoStream == null || videoStream.isEmpty()) {
                log.warn("Video stream is empty or null, user ID: {}", userId);
                return ResponseEntity.badRequest().body("Video stream cannot be empty");
            }

            if (!isValidVideoFormat(videoStream)) {
                log.warn("Unsupported video format, user ID: {}, content type: {}", 
                        userId, videoStream.getContentType());
                return ResponseEntity.badRequest()
                        .body("Unsupported video format. Only MP4 is supported");
            }

            log.debug("Starting video stream navigation, file size: {} bytes, user ID: {}", 
                    videoStream.getSize(), userId);
            String sessionId = realTimeNavigationService.processVideoStream(videoStream, userId, deviceInfo);
            
            log.info("Video stream navigation started successfully, user ID: {}, session ID: {}", 
                    userId, sessionId);
            return ResponseEntity.ok(sessionId);
        } catch (Exception e) {
            log.error("Video stream navigation startup failed, user ID: {}, error: {}", 
                    userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Video stream processing failed: " + e.getMessage());
        }
    }

    /**
     * Support concurrent requests from multiple users
     * Provides batch processing interface to improve concurrent processing capability
     *
     * @param requests batch multimodal request list
     * @return batch processing results
     */
    @PostMapping("/batch-process")
    public CompletableFuture<ResponseEntity<BatchProcessingResult>> supportConcurrentRequests(
            @RequestBody BatchMultimodalRequest requests) {
        log.info("Received batch multimodal input processing request, count: {}", 
                requests.getRequests().size());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SingleProcessingResult> results = requests.getRequests()
                        .stream()
                        .map(this::processSingleRequest)
                        .collect(Collectors.toList());
                
                long successCount = results.stream()
                        .filter(SingleProcessingResult::isSuccess)
                        .count();
                
                BatchProcessingResult batchResult = new BatchProcessingResult(results, successCount);
                log.info("Batch processing completed successfully, success rate: {}/{}", 
                        successCount, results.size());
                
                return ResponseEntity.ok(batchResult);
            } catch (Exception e) {
                log.error("Batch processing failed, error: {}", e.getMessage(), e);
                BatchProcessingResult errorResult = new BatchProcessingResult(
                        List.of(new SingleProcessingResult(false, "Batch processing failed: " + e.getMessage())), 
                        0);
                return ResponseEntity.internalServerError().body(errorResult);
            }
        }, asyncTaskExecutor);
    }

    /**
     * Validate image file format
     *
     * @param file uploaded file
     * @return whether the file format is supported
     */
    private boolean isValidImageFormat(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null) {
            return false;
        }
        
        String contentType = file.getContentType().toLowerCase();
        return contentType.equals("image/jpeg") || contentType.equals("image/png");
    }

    /**
     * Validate video file format
     *
     * @param file uploaded file
     * @return whether the video format is supported
     */
    private boolean isValidVideoFormat(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null) {
            return false;
        }
        
        String contentType = file.getContentType().toLowerCase();
        return contentType.equals("video/mp4");
    }

    /**
     * Process single multimodal request
     *
     * @param request single multimodal request
     * @return processing result
     */
    private SingleProcessingResult processSingleRequest(SingleMultimodalRequest request) {
        if (request == null) {
            log.warn("Received null single multimodal request");
            return new SingleProcessingResult(false, "Request cannot be null");
        }
        
        try {
            String inputType = request.getInputType();
            if (inputType == null || inputType.trim().isEmpty()) {
                log.warn("Input type is empty for user ID: {}", request.getUserId());
                return new SingleProcessingResult(false, "Input type cannot be empty");
            }

            String result;
            switch (inputType.toLowerCase()) {
                case "image":
                    if (request.getFile() == null) {
                        return new SingleProcessingResult(false, "Image file is required");
                    }
                    result = multimodalInputService.parseImageContent(request.getFile(), request.getUserId());
                    break;
                case "text":
                    if (request.getTextDescription() == null) {
                        return new SingleProcessingResult(false, "Text description is required");
                    }
                    result = multimodalInputService.extractTextVideoFeatures(
                            request.getTextDescription(), request.getUserId());
                    break;
                case "video":
                    if (request.getFile() == null) {
                        return new SingleProcessingResult(false, "Video file is required");
                    }
                    result = realTimeNavigationService.processVideoStream(
                            request.getFile(), request.getUserId(), request.getDeviceInfo());
                    break;
                default:
                    log.warn("Unsupported input type: {}, user ID: {}", inputType, request.getUserId());
                    return new SingleProcessingResult(false, "Unsupported input type: " + inputType);
            }
            
            log.debug("Single request processed successfully, type: {}, user ID: {}", 
                    inputType, request.getUserId());
            return new SingleProcessingResult(true, result);
        } catch (Exception e) {
            log.error("Single request processing failed, user ID: {}, input type: {}, error: {}", 
                    request.getUserId(), request.getInputType(), e.getMessage(), e);
            return new SingleProcessingResult(false, "Processing failed: " + e.getMessage());
        }
    }

    // DTO Classes

    /**
     * DTO for single multimodal request
     */
    public static class SingleMultimodalRequest {
        private String inputType;
        private MultipartFile file;
        private String textDescription;
        private Long userId;
        private String deviceInfo;

        public String getInputType() { 
            return inputType; 
        }
        
        public void setInputType(String inputType) { 
            this.inputType = inputType; 
        }
        
        public MultipartFile getFile() { 
            return file; 
        }
        
        public void setFile(MultipartFile file) { 
            this.file = file; 
        }
        
        public String getTextDescription() { 
            return textDescription; 
        }
        
        public void setTextDescription(String textDescription) { 
            this.textDescription = textDescription; 
        }
        
        public Long getUserId() { 
            return userId; 
        }
        
        public void setUserId(Long userId) { 
            this.userId = userId; 
        }
        
        public String getDeviceInfo() { 
            return deviceInfo; 
        }
        
        public void setDeviceInfo(String deviceInfo) { 
            this.deviceInfo = deviceInfo; 
        }
    }

    /**
     * DTO for batch multimodal request
     */
    public static class BatchMultimodalRequest {
        private List<SingleMultimodalRequest> requests;

        public List<SingleMultimodalRequest> getRequests() { 
            return requests; 
        }
        
        public void setRequests(List<SingleMultimodalRequest> requests) { 
            this.requests = requests; 
        }
    }

    /**
     * DTO for single processing result
     */
    public static class SingleProcessingResult {
        private final boolean success;
        private final String result;
        private final String errorMessage;

        public SingleProcessingResult(boolean success, String message) {
            this.success = success;
            if (success) {
                this.result = message;
                this.errorMessage = null;
            } else {
                this.result = null;
                this.errorMessage = message;
            }
        }

        public boolean isSuccess() { 
            return success; 
        }
        
        public String getResult() { 
            return result; 
        }
        
        public String getErrorMessage() { 
            return errorMessage; 
        }
    }

    /**
     * DTO for batch processing result
     */
    public static class BatchProcessingResult {
        private final List<SingleProcessingResult> results;
        private final long successCount;
        private final long totalCount;

        public BatchProcessingResult(List<SingleProcessingResult> results, long successCount) {
            this.results = results;
            this.successCount = successCount;
            this.totalCount = results.size();
        }

        public List<SingleProcessingResult> getResults() { 
            return results; 
        }
        
        public long getSuccessCount() { 
            return successCount; 
        }
        
        public long getTotalCount() { 
            return totalCount; 
        }
        
        public double getSuccessRate() { 
            return totalCount > 0 ? (double) successCount / totalCount : 0.0; 
        }
    }
}


// 内容由AI生成，仅供参考