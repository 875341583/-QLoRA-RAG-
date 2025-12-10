package com.example.navigation.domain.entity;

import lombok.Data;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Navigation path domain entity class
 * Represents system-generated navigation path information, supports dynamic updates and AR display
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Data
public class NavigationPath {
    
    /**
     * Path unique identifier
     */
    private Long id;
    
    /**
     * Path point sequence, containing a series of coordinate points
     * Each point represents a key location on the navigation path
     */
    private List<Point> pathPoints;
    
    /**
     * Distance estimate in meters
     * Represents the total distance from start to end point
     */
    private Double distanceEstimate;
    
    /**
     * Estimated time in seconds
     * Represents the time required to complete the entire navigation path
     */
    private Integer estimatedTime;
    
    /**
     * Obstacle information
     * JSON format string describing obstacle positions and types along the path
     */
    private String obstacleInfo;
    
    /**
     * Path creation time
     */
    private Date createTime;
    
    /**
     * Path last update time
     */
    private Date updateTime;
    
    /**
     * Path status
     * ACTIVE: active path, INACTIVE: inactive path, EXPIRED: expired path
     */
    private String status;
    
    /**
     * Associated user ID
     */
    private Long userId;
    
    /**
     * Associated venue ID
     */
    private Long venueId;
    
    /**
     * Path version number
     * Used to support dynamic updates and version management of paths
     */
    private Integer version;
    
    /**
     * Default constructor
     * Initializes default values and status
     */
    public NavigationPath() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.status = "ACTIVE";
        this.version = 1;
        this.distanceEstimate = 0.0;
        this.estimatedTime = 0;
        this.pathPoints = new ArrayList<>();
        this.obstacleInfo = "{}";
    }
    
    /**
     * Parameterized constructor
     * 
     * @param pathPoints path point sequence
     * @param distanceEstimate distance estimate
     * @param estimatedTime estimated time
     * @param obstacleInfo obstacle information
     * @param userId user ID
     * @param venueId venue ID
     */
    public NavigationPath(List<Point> pathPoints, Double distanceEstimate, 
                         Integer estimatedTime, String obstacleInfo, 
                         Long userId, Long venueId) {
        this();
        this.pathPoints = pathPoints != null ? new ArrayList<>(pathPoints) : new ArrayList<>();
        this.distanceEstimate = distanceEstimate;
        this.estimatedTime = estimatedTime;
        this.obstacleInfo = obstacleInfo != null ? obstacleInfo : "{}";
        this.userId = userId;
        this.venueId = venueId;
    }

    /**
     * Inner Point class representing a coordinate point on the path
     */
    @Data
    public static class Point {
        
        /**
         * X coordinate
         */
        private Double x;
        
        /**
         * Y coordinate
         */
        private Double y;
        
        /**
         * Z coordinate (optional, for 3D navigation)
         */
        private Double z;
        
        /**
         * Point description information
         */
        private String description;
        
        /**
         * Default constructor
         */
        public Point() {
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
            this.description = "";
        }
        
        /**
         * Parameterized constructor
         * 
         * @param x X coordinate
         * @param y Y coordinate
         * @param z Z coordinate
         * @param description point description
         */
        public Point(Double x, Double y, Double z, String description) {
            this.x = x != null ? x : 0.0;
            this.y = y != null ? y : 0.0;
            this.z = z != null ? z : 0.0;
            this.description = description != null ? description : "";
        }
        
        /**
         * 2D coordinate point constructor
         * 
         * @param x X coordinate
         * @param y Y coordinate
         * @param description point description
         */
        public Point(Double x, Double y, String description) {
            this(x, y, 0.0, description);
        }
        
        /**
         * Convert to string representation
         * 
         * @return coordinate point string
         */
        @Override
        public String toString() {
            return String.format("Point(%.2f, %.2f, %.2f)", x, y, z);
        }
    }
    
    /**
     * Validate if the path is valid
     * Check if path points are not empty, distance and time are non-negative numbers
     * 
     * @return true if path is valid, false otherwise
     */
    public boolean isValid() {
        return pathPoints != null && !pathPoints.isEmpty() &&
               distanceEstimate != null && distanceEstimate >= 0 &&
               estimatedTime != null && estimatedTime >= 0;
    }
    
    /**
     * Get the number of path points
     * 
     * @return total number of path points
     */
    public int getPointCount() {
        return pathPoints != null ? pathPoints.size() : 0;
    }
    
    /**
     * Update path status
     * 
     * @param newStatus new status
     */
    public void updateStatus(String newStatus) {
        if (newStatus != null) {
            this.status = newStatus;
            this.updateTime = new Date();
        }
    }
    
    /**
     * Increment path version number
     * Used for version management during path updates
     */
    public void incrementVersion() {
        this.version++;
        this.updateTime = new Date();
    }
    
    /**
     * Add a point to the path
     * 
     * @param point the point to add
     */
    public void addPoint(Point point) {
        if (point != null) {
            if (this.pathPoints == null) {
                this.pathPoints = new ArrayList<>();
            }
            pathPoints.add(point);
            this.updateTime = new Date();
        }
    }
    
    /**
     * Remove a point from the path by index
     * 
     * @param index the index of the point to remove
     * @return true if removal was successful, false otherwise
     */
    public boolean removePoint(int index) {
        if (this.pathPoints == null) {
            this.pathPoints = new ArrayList<>();
            return false;
        }
        if (index >= 0 && index < pathPoints.size()) {
            pathPoints.remove(index);
            this.updateTime = new Date();
            return true;
        }
        return false;
    }
    
    /**
     * Convert to simplified string representation
     * 
     * @return basic path information string
     */
    @Override
    public String toString() {
        return String.format("NavigationPath{id=%d, points=%d, distance=%.2fm, time=%ds, status=%s}", 
                           id, getPointCount(), distanceEstimate, estimatedTime, status);
    }
}


// 内容由AI生成，仅供参考