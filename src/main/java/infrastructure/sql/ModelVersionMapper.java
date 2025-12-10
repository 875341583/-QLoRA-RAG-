package com.navigation.system.infrastructure.sql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.navigation.system.domain.entity.ModelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Model version data access layer implementation class.
 * Responsible for database operations of model version data, including insert, query, update, etc.
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Mapper
public interface ModelVersionMapper extends BaseMapper<ModelVersion> {
    
    /**
     * Insert model version record.
     * 
     * @param modelVersion model version entity object
     * @return number of successfully inserted records
     */
    @Override
    int insert(ModelVersion modelVersion);
    
    /**
     * Query deployment status by version ID.
     * 
     * @param versionId version ID
     * @return deployment status string, null if not found
     */
    @Select("SELECT deployment_status FROM model_version WHERE id = #{versionId}")
    String selectDeploymentStatusById(@Param("versionId") Long versionId);
    
    /**
     * Query model version list sorted by accuracy rate in descending order.
     * 
     * @return model version list sorted by accuracy rate descending
     */
    @Select("SELECT * FROM model_version ORDER BY accuracy_rate DESC")
    List<ModelVersion> selectAllOrderByAccuracyRateDesc();
    
    /**
     * Update model version accuracy rate by ID.
     * 
     * @param versionId version ID
     * @param accuracyRate new accuracy rate value
     * @return number of successfully updated records
     */
    @Update("UPDATE model_version SET accuracy_rate = #{accuracyRate}, update_time = CURRENT_TIMESTAMP WHERE id = #{versionId}")
    int updateAccuracyRateById(@Param("versionId") Long versionId, 
                              @Param("accuracyRate") Double accuracyRate);
    
    /**
     * Update model version deployment status by ID.
     * 
     * @param versionId version ID
     * @param deploymentStatus new deployment status
     * @return number of successfully updated records
     */
    @Update("UPDATE model_version SET deployment_status = #{deploymentStatus}, update_time = CURRENT_TIMESTAMP WHERE id = #{versionId}")
    int updateDeploymentStatusById(@Param("versionId") Long versionId, 
                                  @Param("deploymentStatus") String deploymentStatus);
    
    /**
     * Query model version list by deployment status.
     * 
     * @param deploymentStatus deployment status
     * @return model version list matching the deployment status
     */
    @Select("SELECT * FROM model_version WHERE deployment_status = #{deploymentStatus} ORDER BY creation_time DESC")
    List<ModelVersion> selectByDeploymentStatus(@Param("deploymentStatus") String deploymentStatus);
    
    /**
     * Query the latest model version.
     * 
     * @return latest model version entity, null if no records exist
     */
    @Select("SELECT * FROM model_version ORDER BY creation_time DESC LIMIT 1")
    ModelVersion selectLatestVersion();
    
    /**
     * Query model versions with accuracy rate greater than or equal to threshold.
     * 
     * @param accuracyThreshold accuracy rate threshold
     * @return model version list meeting accuracy requirement
     */
    @Select("SELECT * FROM model_version WHERE accuracy_rate >= #{accuracyThreshold} ORDER BY accuracy_rate DESC")
    List<ModelVersion> selectByAccuracyThreshold(@Param("accuracyThreshold") Double accuracyThreshold);
    
    /**
     * Check if model version exists by version number.
     * 
     * @param versionNumber version number
     * @return true if exists, false otherwise
     */
    @Select("SELECT COUNT(*) > 0 FROM model_version WHERE version_number = #{versionNumber}")
    boolean existsByVersionNumber(@Param("versionNumber") String versionNumber);
}


// 内容由AI生成，仅供参考