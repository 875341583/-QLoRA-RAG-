package com.chinatelecom.navigation.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * Multimodal input entity class
 * Represents user's multimodal input data including images, text, video, etc.
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class MultimodalInput implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Input type enumeration
     */
    public enum InputType {
        IMAGE("图片"),
        TEXT("文本"), 
        VIDEO("视频");

        private final String displayName;

        InputType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Processing status enumeration
     */
    public enum ProcessingStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        FAILED("失败");

        private final String displayName;

        ProcessingStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Primary key ID
     */
    private Long id;

    /**
     * Input type: IMAGE, TEXT, VIDEO
     */
    private InputType inputType;

    /**
     * Data content: image file path, text content, video file path
     */
    private String dataContent;

    /**
     * Timestamp: records the time when the input data was generated
     */
    private Date timestamp;

    /**
     * User ID: associates with user table
     */
    private Long userId;

    /**
     * Processing status: PENDING, PROCESSING, COMPLETED, FAILED
     */
    private ProcessingStatus processingStatus;

    /**
     * Processing result: stores parsed location information or feature vectors
     */
    private String processingResult;

    /**
     * Creation time: records when the entity was created in the system
     */
    private Date createTime;

    /**
     * Update time: records when the entity was last updated
     */
    private Date updateTime;

    /**
     * Retry count: tracks how many times processing has been attempted
     */
    private Integer retryCount = 0;

    /**
     * Maximum allowed retry attempts
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Default constructor
     */
    public MultimodalInput() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.processingStatus = ProcessingStatus.PENDING;
        this.retryCount = 0;
    }

    /**
     * Parameterized constructor
     * 
     * @param inputType input type
     * @param dataContent data content
     * @param userId user ID
     * @param timestamp data generation timestamp
     */
    public MultimodalInput(InputType inputType, String dataContent, Long userId, Date timestamp) {
        this();
        this.inputType = inputType;
        this.dataContent = dataContent;
        this.userId = userId;
        this.timestamp = timestamp != null ? timestamp : new Date();
    }

    /**
     * Update processing status with validation
     * 
     * @param status new processing status
     * @param result processing result
     * @throws IllegalArgumentException if status is null or business rules are violated
     */
    public void updateProcessingStatus(ProcessingStatus status, String result) {
        if (status == null) {
            throw new IllegalArgumentException("Processing status cannot be null");
        }
        
        // Validate business rules for completed/failed status
        if (status == ProcessingStatus.COMPLETED || status == ProcessingStatus.FAILED) {
            if (result == null || result.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Processing result cannot be null or empty for status: " + status);
            }
        }
        
        // Update retry count for failed status
        if (status == ProcessingStatus.FAILED) {
            this.retryCount = (this.retryCount != null ? this.retryCount : 0) + 1;
        }
        
        this.processingStatus = status;
        this.processingResult = result;
        this.updateTime = new Date();
    }

    /**
     * Check if the input can be processed
     * Allows processing for PENDING status or FAILED status with retry attempts remaining
     * 
     * @return true if processable, false otherwise
     */
    public boolean isProcessable() {
        if (ProcessingStatus.PENDING.equals(this.processingStatus)) {
            return true;
        }
        
        if (ProcessingStatus.FAILED.equals(this.processingStatus)) {
            return (this.retryCount != null ? this.retryCount : 0) < MAX_RETRY_ATTEMPTS;
        }
        
        return false;
    }

    /**
     * Check if processing is completed successfully
     * 
     * @return true if completed successfully, false otherwise
     */
    public boolean isCompleted() {
        return ProcessingStatus.COMPLETED.equals(this.processingStatus);
    }

    /**
     * Check if processing failed
     * 
     * @return true if failed, false otherwise
     */
    public boolean isFailed() {
        return ProcessingStatus.FAILED.equals(this.processingStatus);
    }

    /**
     * Check if maximum retry attempts have been exceeded
     * 
     * @return true if no more retries allowed, false otherwise
     */
    public boolean isMaxRetryExceeded() {
        return ProcessingStatus.FAILED.equals(this.processingStatus) && 
               (this.retryCount != null ? this.retryCount : 0) >= MAX_RETRY_ATTEMPTS;
    }

    /**
     * Get input type display name
     * 
     * @return input type display name
     */
    public String getInputTypeDisplayName() {
        return inputType != null ? inputType.getDisplayName() : "未知类型";
    }

    /**
     * Get processing status display name
     * 
     * @return processing status display name
     */
    public String getProcessingStatusDisplayName() {
        return processingStatus != null ? processingStatus.getDisplayName() : "未知状态";
    }

    /**
     * Validate the entity for basic integrity
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return inputType != null && 
               dataContent != null && !dataContent.trim().isEmpty() &&
               userId != null && userId > 0 &&
               timestamp != null &&
               createTime != null &&
               updateTime != null &&
               processingStatus != null &&
               retryCount != null && retryCount >= 0;
    }

    /**
     * Mark as processing
     * 
     * @throws IllegalStateException if not in a processable state
     */
    public void markAsProcessing() {
        if (!isProcessable()) {
            throw new IllegalStateException(
                "Cannot mark as processing. Current status: " + this.processingStatus + 
                ", Retry count: " + this.retryCount);
        }
        updateProcessingStatus(ProcessingStatus.PROCESSING, null);
    }

    /**
     * Mark as completed with result
     * 
     * @param result processing result
     * @throws IllegalArgumentException if result is null or empty
     */
    public void markAsCompleted(String result) {
        if (result == null || result.trim().isEmpty()) {
            throw new IllegalArgumentException("Processing result cannot be null or empty");
        }
        updateProcessingStatus(ProcessingStatus.COMPLETED, result);
    }

    /**
     * Mark as failed with error message
     * 
     * @param errorMessage error message
     * @throws IllegalArgumentException if error message is null or empty
     */
    public void markAsFailed(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        updateProcessingStatus(ProcessingStatus.FAILED, errorMessage);
    }

    /**
     * Reset for retry - clears processing result and sets status to PENDING
     * Only applicable for FAILED status with retry attempts remaining
     * 
     * @throws IllegalStateException if not eligible for retry
     */
    public void resetForRetry() {
        if (!ProcessingStatus.FAILED.equals(this.processingStatus)) {
            throw new IllegalStateException("Reset for retry only applicable for FAILED status");
        }
        
        if (isMaxRetryExceeded()) {
            throw new IllegalStateException("Maximum retry attempts exceeded");
        }
        
        this.processingStatus = ProcessingStatus.PENDING;
        this.processingResult = null;
        this.updateTime = new Date();
    }

    /**
     * Get remaining retry attempts
     * 
     * @return number of remaining retry attempts
     */
    public int getRemainingRetryAttempts() {
        if (!ProcessingStatus.FAILED.equals(this.processingStatus)) {
            return 0;
        }
        return Math.max(0, MAX_RETRY_ATTEMPTS - (this.retryCount != null ? this.retryCount : 0));
    }
}


// 内容由AI生成，仅供参考