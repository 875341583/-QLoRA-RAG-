package com.navigation.system.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FAISS vector index configuration class.
 * Responsible for configuring FAISS vector index construction and query parameters.
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Configuration
@ConfigurationProperties(prefix = "faiss")
public class FAISSConfig {
    
    /**
     * Index type configuration.
     * Default uses IVF_FLAT index, suitable for medium-scale datasets.
     */
    private String indexType = "IVF_FLAT";
    
    /**
     * Vector dimension size.
     * Default 512 dimensions, compatible with CLIP model output features.
     */
    private Integer dimensionSize = 512;
    
    /**
     * Similarity algorithm.
     * Default uses inner product similarity, suitable for vector retrieval scenarios.
     */
    private String similarityAlgorithm = "INNER_PRODUCT";
    
    /**
     * Number of IVF clustering centers.
     * Default 256 clusters, balancing retrieval accuracy and performance.
     */
    private Integer nlist = 256;
    
    /**
     * Number of clusters to probe during search.
     * Default probes 10 clusters, optimizing retrieval efficiency.
     */
    private Integer nprobe = 10;
    
    /**
     * Index file storage path.
     * Default stores in system temporary directory.
     */
    private String indexFilePath = "/tmp/faiss_index";
    
    /**
     * Whether to enable GPU acceleration.
     * Default disabled, enable when GPU resources are sufficient.
     */
    private Boolean gpuAcceleration = false;
    
    /**
     * Batch processing vector count.
     * Default processes 1000 vectors per batch, optimizing memory usage.
     */
    private Integer batchSize = 1000;
    
    /**
     * Number of threads for index construction.
     * Default 4 threads, adjust based on CPU core count.
     */
    private Integer buildThreads = 4;
    
    /**
     * Number of search results to return.
     * Default returns 10 most similar results.
     */
    private Integer searchK = 10;

    // Getter and Setter methods
    
    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public Integer getDimensionSize() {
        return dimensionSize;
    }

    public void setDimensionSize(Integer dimensionSize) {
        this.dimensionSize = dimensionSize;
    }

    public String getSimilarityAlgorithm() {
        return similarityAlgorithm;
    }

    public void setSimilarityAlgorithm(String similarityAlgorithm) {
        this.similarityAlgorithm = similarityAlgorithm;
    }

    public Integer getNlist() {
        return nlist;
    }

    public void setNlist(Integer nlist) {
        this.nlist = nlist;
    }

    public Integer getNprobe() {
        return nprobe;
    }

    public void setNprobe(Integer nprobe) {
        this.nprobe = nprobe;
    }

    public String getIndexFilePath() {
        return indexFilePath;
    }

    public void setIndexFilePath(String indexFilePath) {
        this.indexFilePath = indexFilePath;
    }

    public Boolean getGpuAcceleration() {
        return gpuAcceleration;
    }

    public void setGpuAcceleration(Boolean gpuAcceleration) {
        this.gpuAcceleration = gpuAcceleration;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getBuildThreads() {
        return buildThreads;
    }

    public void setBuildThreads(Integer buildThreads) {
        this.buildThreads = buildThreads;
    }

    public Integer getSearchK() {
        return searchK;
    }

    public void setSearchK(Integer searchK) {
        this.searchK = searchK;
    }

    /**
     * Validates configuration parameter legality.
     * 
     * @return true if configuration is valid
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    public boolean validate() {
        if (dimensionSize == null || dimensionSize <= 0 || dimensionSize > 4096) {
            throw new IllegalArgumentException("Vector dimension size must be between 1-4096");
        }
        if (nlist == null || nlist <= 0 || nlist > 65536) {
            throw new IllegalArgumentException("Number of clustering centers must be between 1-65536");
        }
        if (nprobe == null || nprobe <= 0) {
            throw new IllegalArgumentException("Number of clusters to probe must be greater than 0");
        }
        if (nlist != null && nprobe > nlist) {
            throw new IllegalArgumentException("Number of clusters to probe must be less than or equal to total clusters");
        }
        if (batchSize == null || batchSize <= 0 || batchSize > 100000) {
            throw new IllegalArgumentException("Batch processing size must be between 1-100000");
        }
        if (buildThreads == null || buildThreads <= 0 || buildThreads > 64) {
            throw new IllegalArgumentException("Number of build threads must be between 1-64");
        }
        if (searchK == null || searchK <= 0 || searchK > 1000) {
            throw new IllegalArgumentException("Number of search results must be between 1-1000");
        }
        return true;
    }

    /**
     * Generates configuration summary information.
     * 
     * @return configuration information string
     */
    @Override
    public String toString() {
        return String.format(
            "FAISSConfig{indexType='%s', dimensionSize=%d, similarityAlgorithm='%s', " +
            "nlist=%d, nprobe=%d, gpuAcceleration=%s, batchSize=%d, buildThreads=%d, searchK=%d}",
            indexType, dimensionSize, similarityAlgorithm, nlist, nprobe, 
            gpuAcceleration, batchSize, buildThreads, searchK
        );
    }
}


// 内容由AI生成，仅供参考