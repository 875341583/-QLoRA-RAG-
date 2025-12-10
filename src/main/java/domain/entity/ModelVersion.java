package com.chinatelecom.navigation.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * Model version entity class
 * Represents QLoRA fine-tuned model version information for model lifecycle management
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ModelVersion implements Serializable {

    private static final long serialVersionUID = 1L;
    
    /**
     * Deployment accuracy threshold (93.5% as per business requirement)
     */
    private static final Double DEPLOYMENT_ACCURACY_THRESHOLD = 0.935;

    /**
     * Primary key ID
     */
    private Long id;

    /**
     * Version number, format: v1.0.0
     */
    private String versionNumber;

    /**
     * Training data size
     */
    private Integer trainingDataSize;

    /**
     * Accuracy rate, range: 0.0-1.0
     */
    private Double accuracyRate;

    /**
     * Deployment status
     */
    private DeploymentStatus deploymentStatus;

    /**
     * Creation time
     */
    private Date creationTime;

    /**
     * Update time
     */
    private Date updateTime;

    /**
     * Model file path
     */
    private String modelFilePath;

    /**
     * Training task ID
     */
    private String trainingTaskId;

    /**
     * Model description
     */
    private String description;

    /**
     * Deployment status enum
     * Available values: TRAINING, READY, DEPLOYED, ROLLBACK
     */
    public enum DeploymentStatus {
        TRAINING,
        READY, 
        DEPLOYED,
        ROLLBACK
    }

    /**
     * Performance level enum based on accuracy rate
     * EXCELLENT: >= 95% (suitable for production)
     * GOOD: >= 90% (suitable for testing)
     * FAIR: >= 85% (needs improvement)
     * POOR: < 85% (requires retraining)
     * UNKNOWN: accuracy rate not available
     */
    public enum PerformanceLevel {
        EXCELLENT(0.95),
        GOOD(0.90),
        FAIR(0.85),
        POOR(0.0),
        UNKNOWN(-1.0);

        private final double threshold;

        PerformanceLevel(double threshold) {
            this.threshold = threshold;
        }

        public double getThreshold() {
            return threshold;
        }
    }

    /**
     * Default constructor
     */
    public ModelVersion() {
        this.creationTime = new Date();
        this.updateTime = new Date();
        this.deploymentStatus = DeploymentStatus.TRAINING;
        this.trainingDataSize = 0;
        this.accuracyRate = 0.0;
    }

    /**
     * Parameterized constructor
     * 
     * @param versionNumber version number
     * @param trainingDataSize training data size
     * @param accuracyRate accuracy rate
     */
    public ModelVersion(String versionNumber, Integer trainingDataSize, Double accuracyRate) {
        this();
        this.versionNumber = versionNumber;
        this.trainingDataSize = trainingDataSize;
        this.accuracyRate = accuracyRate;
    }

    /**
     * Check if the model is deployable
     * Models are deployable when status is READY, accuracy meets threshold, and model file exists
     * 
     * @return true if deployable, false otherwise
     */
    public boolean isDeployable() {
        return DeploymentStatus.READY.equals(deploymentStatus) 
                && accuracyRate != null && accuracyRate >= DEPLOYMENT_ACCURACY_THRESHOLD
                && modelFilePath != null && !modelFilePath.trim().isEmpty();
    }

    /**
     * Check if the model is in training status
     * 
     * @return true if training, false otherwise
     */
    public boolean isTraining() {
        return DeploymentStatus.TRAINING.equals(deploymentStatus);
    }

    /**
     * Check if the model is deployed
     * 
     * @return true if deployed, false otherwise
     */
    public boolean isDeployed() {
        return DeploymentStatus.DEPLOYED.equals(deploymentStatus);
    }

    /**
     * Check if the model can be rolled back
     * 
     * @return true if rollback is possible, false otherwise
     */
    public boolean canRollback() {
        return DeploymentStatus.DEPLOYED.equals(deploymentStatus) 
                || DeploymentStatus.ROLLBACK.equals(deploymentStatus);
    }

    /**
     * Update model information with validation
     * 
     * @param accuracyRate new accuracy rate (nullable)
     * @param deploymentStatus new deployment status (required)
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void updateModelInfo(Double accuracyRate, DeploymentStatus deploymentStatus) {
        if (deploymentStatus == null) {
            throw new IllegalArgumentException("Deployment status cannot be null.");
        }
        
        if (accuracyRate != null && (accuracyRate < 0.0 || accuracyRate > 1.0)) {
            throw new IllegalArgumentException("Accuracy rate must be between 0.0 and 1.0.");
        }
        
        this.accuracyRate = accuracyRate;
        this.deploymentStatus = deploymentStatus;
        this.updateTime = new Date();
    }

    /**
     * Validate model version information completeness
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return versionNumber != null && !versionNumber.trim().isEmpty() &&
               trainingDataSize != null && trainingDataSize >= 0 &&
               accuracyRate != null && accuracyRate >= 0.0 && accuracyRate <= 1.0 &&
               deploymentStatus != null;
    }

    /**
     * Get model performance level based on accuracy rate
     * Performance levels are defined based on industry standards for ML model evaluation
     * 
     * @return performance level enum
     */
    public PerformanceLevel getPerformanceLevel() {
        if (accuracyRate == null) {
            return PerformanceLevel.UNKNOWN;
        }
        
        if (accuracyRate >= PerformanceLevel.EXCELLENT.getThreshold()) {
            return PerformanceLevel.EXCELLENT;
        } else if (accuracyRate >= PerformanceLevel.GOOD.getThreshold()) {
            return PerformanceLevel.GOOD;
        } else if (accuracyRate >= PerformanceLevel.FAIR.getThreshold()) {
            return PerformanceLevel.FAIR;
        } else {
            return PerformanceLevel.POOR;
        }
    }

    /**
     * Get deployment accuracy threshold
     * 
     * @return deployment accuracy threshold
     */
    public static Double getDeploymentAccuracyThreshold() {
        return DEPLOYMENT_ACCURACY_THRESHOLD;
    }

    @Override
    public String toString() {
        return "ModelVersion{" +
                "id=" + id +
                ", versionNumber='" + versionNumber + '\'' +
                ", trainingDataSize=" + trainingDataSize +
                ", accuracyRate=" + accuracyRate +
                ", deploymentStatus=" + deploymentStatus +
                ", creationTime=" + creationTime +
                ", updateTime=" + updateTime +
                ", modelFilePath='" + modelFilePath + '\'' +
                ", trainingTaskId='" + trainingTaskId + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}


// 内容由AI生成，仅供参考