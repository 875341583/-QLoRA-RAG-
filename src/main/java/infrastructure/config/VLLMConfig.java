package com.navigation.system.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Set;

/**
 * vLLM inference engine configuration class.
 * Responsible for configuring connection parameters and runtime parameters for the vLLM inference engine.
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Configuration
@ConfigurationProperties(prefix = "vllm")
public class VLLMConfig {
    
    private static final Set<String> VALID_GPU_MEMORY_STRATEGIES = Set.of("auto", "balanced", "high-performance");
    private static final Set<String> VALID_LOG_LEVELS = Set.of("DEBUG", "INFO", "WARN", "ERROR");
    
    /**
     * Model file path, default is empty string.
     */
    private String modelPath = "";
    
    /**
     * Concurrent processing threads, default is 4.
     */
    private Integer concurrentThreads = 4;
    
    /**
     * GPU memory allocation strategy, default is "auto".
     * Valid values: auto, balanced, high-performance
     */
    private String gpuMemoryAllocation = "auto";
    
    /**
     * Batch size, default is 32.
     */
    private Integer batchSize = 32;
    
    /**
     * Maximum sequence length, default is 2048.
     */
    private Integer maxSequenceLength = 2048;
    
    /**
     * Model loading timeout in seconds, default is 300.
     */
    private Integer modelLoadTimeout = 300;
    
    /**
     * Inference request timeout in seconds, default is 60.
     */
    private Integer inferenceTimeout = 60;
    
    /**
     * Whether to enable tensor parallelism, default is false.
     */
    private Boolean enableTensorParallelism = false;
    
    /**
     * Tensor parallelism degree, default is 1.
     */
    private Integer tensorParallelDegree = 1;
    
    /**
     * Log level, default is "INFO".
     */
    private String logLevel = "INFO";

    public VLLMConfig() {
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public Integer getConcurrentThreads() {
        return concurrentThreads;
    }

    public void setConcurrentThreads(Integer concurrentThreads) {
        this.concurrentThreads = concurrentThreads;
    }

    public String getGpuMemoryAllocation() {
        return gpuMemoryAllocation;
    }

    public void setGpuMemoryAllocation(String gpuMemoryAllocation) {
        this.gpuMemoryAllocation = gpuMemoryAllocation;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getMaxSequenceLength() {
        return maxSequenceLength;
    }

    public void setMaxSequenceLength(Integer maxSequenceLength) {
        this.maxSequenceLength = maxSequenceLength;
    }

    public Integer getModelLoadTimeout() {
        return modelLoadTimeout;
    }

    public void setModelLoadTimeout(Integer modelLoadTimeout) {
        this.modelLoadTimeout = modelLoadTimeout;
    }

    public Integer getInferenceTimeout() {
        return inferenceTimeout;
    }

    public void setInferenceTimeout(Integer inferenceTimeout) {
        this.inferenceTimeout = inferenceTimeout;
    }

    public Boolean getEnableTensorParallelism() {
        return enableTensorParallelism;
    }

    public void setEnableTensorParallelism(Boolean enableTensorParallelism) {
        this.enableTensorParallelism = enableTensorParallelism;
    }

    public Integer getTensorParallelDegree() {
        return tensorParallelDegree;
    }

    public void setTensorParallelDegree(Integer tensorParallelDegree) {
        this.tensorParallelDegree = tensorParallelDegree;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Validates configuration parameters.
     * @return true if configuration is valid, false otherwise
     */
    public boolean validate() {
        if (modelPath == null || modelPath.trim().isEmpty()) {
            return false;
        }
        if (concurrentThreads == null || concurrentThreads <= 0) {
            return false;
        }
        if (gpuMemoryAllocation == null || !VALID_GPU_MEMORY_STRATEGIES.contains(gpuMemoryAllocation)) {
            return false;
        }
        if (batchSize == null || batchSize <= 0) {
            return false;
        }
        if (maxSequenceLength == null || maxSequenceLength <= 0) {
            return false;
        }
        if (modelLoadTimeout == null || modelLoadTimeout <= 0) {
            return false;
        }
        if (inferenceTimeout == null || inferenceTimeout <= 0) {
            return false;
        }
        if (logLevel == null || !VALID_LOG_LEVELS.contains(logLevel)) {
            return false;
        }
        
        // Enhanced tensor parallelism validation
        if (enableTensorParallelism) {
            if (tensorParallelDegree == null || tensorParallelDegree <= 1) {
                return false;
            }
        } else {
            if (tensorParallelDegree == null || tensorParallelDegree != 1) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Generates configuration summary string.
     * @return configuration information summary
     */
    @Override
    public String toString() {
        return "VLLMConfig{" +
                "modelPath='" + modelPath + '\'' +
                ", concurrentThreads=" + concurrentThreads +
                ", gpuMemoryAllocation='" + gpuMemoryAllocation + '\'' +
                ", batchSize=" + batchSize +
                ", maxSequenceLength=" + maxSequenceLength +
                ", modelLoadTimeout=" + modelLoadTimeout +
                ", inferenceTimeout=" + inferenceTimeout +
                ", enableTensorParallelism=" + enableTensorParallelism +
                ", tensorParallelDegree=" + tensorParallelDegree +
                ", logLevel='" + logLevel + '\'' +
                '}';
    }
}


// 内容由AI生成，仅供参考