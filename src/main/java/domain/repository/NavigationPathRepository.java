package com.navigation.system.domain.repository;

import com.navigation.system.domain.entity.NavigationPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Navigation Path Repository Interface.
 * Provides storage and retrieval operations for navigation paths, following Domain-Driven Design (DDD) principles.
 * 
 * @author Navigation System Team
 * @version 1.0
 */
@Repository
public interface NavigationPathRepository extends JpaRepository<NavigationPath, Long> {
    
    /**
     * Saves path data.
     * Uses JPA's save method to automatically persist navigation path data.
     *
     * @param navigationPath navigation path entity object
     * @return saved navigation path entity with generated ID
     */
    @Override
    NavigationPath save(NavigationPath navigationPath);
    
    /**
     * Queries the latest path for a specific venue.
     * Retrieves the most recent navigation path record ordered by creation time.
     *
     * @param venueId venue unique identifier
     * @return Optional wrapper for the latest navigation path to avoid null pointer exceptions
     */
    @Query("SELECT np FROM NavigationPath np WHERE np.venueId = :venueId ORDER BY np.creationTime DESC")
    Optional<NavigationPath> findLatestByVenueId(@Param("venueId") Long venueId);
    
    /**
     * Updates paths status by venue.
     * Batch updates status of all paths for a specific venue.
     * Note: Calling service method should be annotated with @Transactional.
     *
     * @param venueId venue unique identifier
     * @param status path status to update (e.g., ACTIVE, INACTIVE, EXPIRED)
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE NavigationPath np SET np.status = :status WHERE np.venueId = :venueId")
    int updateStatusByVenueId(@Param("venueId") Long venueId, @Param("status") String status);
    
    /**
     * Queries historical path records by user ID.
     * Retrieves all navigation paths used by a specific user, sorted by creation time in descending order.
     *
     * @param userId user unique identifier
     * @return list of user's historical navigation paths
     */
    @Query("SELECT np FROM NavigationPath np WHERE np.userId = :userId ORDER BY np.creationTime DESC")
    List<NavigationPath> findByUserId(@Param("userId") Long userId);
    
    /**
     * Queries path list by status.
     * Used for monitoring and managing navigation paths with different statuses.
     *
     * @param status path status (e.g., ACTIVE, INACTIVE, PENDING)
     * @return list of navigation paths matching the status
     */
    List<NavigationPath> findByStatus(String status);
    
    /**
     * Checks if active navigation path exists for a venue.
     * Verifies if a specific venue already has an active navigation path to avoid duplicate creation.
     *
     * @param venueId venue unique identifier
     * @return true if active path exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(np) > 0 THEN true ELSE false END " +
           "FROM NavigationPath np WHERE np.venueId = :venueId AND np.status = 'ACTIVE'")
    boolean existsActivePathByVenueId(@Param("venueId") Long venueId);
    
    /**
     * Finds navigation paths by venue ID and status.
     * Useful for retrieving specific types of paths for a venue.
     *
     * @param venueId venue unique identifier
     * @param status path status to filter by
     * @return list of navigation paths matching the criteria
     */
    List<NavigationPath> findByVenueIdAndStatus(Long venueId, String status);
}


// 内容由AI生成，仅供参考