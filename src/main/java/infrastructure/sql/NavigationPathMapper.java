package com.navigation.system.infrastructure.sql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.navigation.system.domain.entity.NavigationPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Navigation Path Data Mapper Interface
 * Responsible for database operations of navigation path data, including inserting path information,
 * querying the latest path, updating path status, etc.
 * 
 * @author Alex
 * @version 1.2
 * @since 2024
 */
@Mapper
public interface NavigationPathMapper extends BaseMapper<NavigationPath> {
    
    /**
     * Insert path information.
     * Save the navigation path entity to the database.
     *
     * @param navigationPath the navigation path entity object
     * @return the number of rows affected by the insert operation
     */
    @Override
    int insert(NavigationPath navigationPath);
    
    /**
     * Query active path by path ID.
     * Gets the detailed information of the specified active path.
     *
     * @param pathId the unique identifier of the path (primary key)
     * @return the navigation path entity object, returns null if not exists or inactive
     */
    @Select("SELECT * FROM navigation_path WHERE path_id = #{pathId} AND is_active = true")
    NavigationPath selectActivePathById(@Param("pathId") Long pathId);
    
    /**
     * Query the latest active path by venue ID.
     * Gets the latest valid navigation path for the specified venue.
     *
     * @param venueId the unique identifier of the venue
     * @return the navigation path entity object, returns null if not exists
     */
    @Select("SELECT * FROM navigation_path WHERE venue_id = #{venueId} AND is_active = true ORDER BY update_time DESC LIMIT 1")
    NavigationPath selectLatestPathByVenue(@Param("venueId") Long venueId);
    
    /**
     * Query historical path list by user ID.
     * Gets all navigation path records used by the user.
     *
     * @param userId the unique identifier of the user
     * @return list of navigation path entities, sorted by update time in descending order
     */
    @Select("SELECT * FROM navigation_path WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<NavigationPath> selectHistoryPathsByUser(@Param("userId") Long userId);
    
    /**
     * Update path status.
     * Modify the active status of the navigation path, used for path invalidation or reactivation.
     *
     * @param pathId the unique identifier of the path
     * @param isActive whether active status (true-active, false-inactive)
     * @return the number of rows affected by the update operation
     */
    @Update("UPDATE navigation_path SET is_active = #{isActive}, update_time = CURRENT_TIMESTAMP WHERE path_id = #{pathId}")
    int updatePathStatus(@Param("pathId") Long pathId, @Param("isActive") Boolean isActive);
    
    /**
     * Update path status by venue ID.
     * Batch update the status of all paths for the specified venue.
     *
     * @param venueId the unique identifier of the venue
     * @param isActive whether active status (true-active, false-inactive)
     * @return the number of rows affected by the update operation
     */
    @Update("UPDATE navigation_path SET is_active = #{isActive}, update_time = CURRENT_TIMESTAMP WHERE venue_id = #{venueId}")
    int updatePathStatusByVenue(@Param("venueId") Long venueId, @Param("isActive") Boolean isActive);
    
    /**
     * Update path information by venue ID.
     * Batch update path data for all paths in the specified venue.
     *
     * @param venueId the unique identifier of the venue
     * @param navigationPath the navigation path entity containing update fields
     * @return the number of rows affected by the update operation
     */
    @Update("UPDATE navigation_path SET path_points = #{navigationPath.pathPoints}, distance_estimate = #{navigationPath.distanceEstimate}, " +
            "estimated_time = #{navigationPath.estimatedTime}, obstacle_info = #{navigationPath.obstacleInfo}, " +
            "update_time = CURRENT_TIMESTAMP WHERE venue_id = #{venueId}")
    int updatePathByVenue(@Param("venueId") Long venueId, @Param("navigationPath") NavigationPath navigationPath);
    
    /**
     * Soft delete path record.
     * Mark the path as deleted while preserving historical data.
     *
     * @param pathId the unique identifier of the path
     * @return the number of rows affected by the delete operation
     */
    @Update("UPDATE navigation_path SET is_deleted = true, update_time = CURRENT_TIMESTAMP WHERE path_id = #{pathId}")
    int softDeletePath(@Param("pathId") Long pathId);
}


// 内容由AI生成，仅供参考