package com.navigation.system.domain.repository;

import com.navigation.system.domain.entity.ModelVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Model version repository interface.
 * Provides persistence operations and query functions for model version information.
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Repository
public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {
    
    /**
     * Deployment status constant for deployed models.
     */
    String DEPLOYED_STATUS = "DEPLOYED";
    
    /**
     * Finds model versions by deployment status.
     *
     * @param deploymentStatus the deployment status to search for
     * @return list of model versions matching the deployment status
     */
    List<ModelVersion> findByDeploymentStatus(String deploymentStatus);
    
    /**
     * Finds the latest deployed model version.
     *
     * @return the latest deployed model version, empty if none exists
     */
    Optional<ModelVersion> findFirstByDeploymentStatusOrderByCreationTimeDesc(String deploymentStatus);
    
    /**
     * Finds all model versions ordered by accuracy rate in descending order.
     *
     * @return list of model versions sorted by accuracy rate from highest to lowest
     */
    @Query("SELECT mv FROM ModelVersion mv ORDER BY mv.accuracyRate DESC")
    List<ModelVersion> findAllOrderByAccuracyRateDesc();
    
    /**
     * Finds model version by version number.
     *
     * @param versionNumber the version number to search for
     * @return the corresponding model version, empty if not found
     */
    Optional<ModelVersion> findByVersionNumber(String versionNumber);
    
    /**
     * Checks if a model version with the specified version number exists.
     *
     * @param versionNumber the version number to check
     * @return true if exists, false otherwise
     */
    boolean existsByVersionNumber(String versionNumber);
    
    /**
     * Finds model versions by training data size range.
     *
     * @param minSize the minimum training data size (inclusive)
     * @param maxSize the maximum training data size (inclusive)
     * @return list of model versions within the specified size range
     */
    List<ModelVersion> findByTrainingDataSizeBetween(Integer minSize, Integer maxSize);
    
    /**
     * Finds the latest deployed model version using the predefined deployed status.
     *
     * @return the latest deployed model version, empty if none exists
     */
    default Optional<ModelVersion> findLatestDeployedVersion() {
        return findFirstByDeploymentStatusOrderByCreationTimeDesc(DEPLOYED_STATUS);
    }
}


// 内容由AI生成，仅供参考