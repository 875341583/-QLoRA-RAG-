package com.navigation.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * Knowledge Base Entity Class
 * Represents knowledge base content in RAG architecture, supporting incremental updates and fast retrieval
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("knowledge_base")
public class KnowledgeBase {
    
    /**
     * Primary key ID, auto-increment
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * Venue map data, storing JSON format map information
     * Contains venue layout, path points, obstacles and other detailed information
     */
    private String venueMapData;
    
    /**
     * Rule text, storing venue-related rules and constraints
     * Such as access permissions, opening hours, special area descriptions, etc.
     */
    private String ruleText;
    
    /**
     * Vector index, storing FAISS built vector index data
     * Used for fast similarity search and semantic matching
     */
    private String vectorIndex;
    
    /**
     * Update time, recording the timestamp of the last knowledge base update
     * Used for version control and incremental synchronization
     */
    private LocalDateTime updateTime;
    
    /**
     * Creation time, recording the initial creation time of the knowledge base
     */
    private LocalDateTime createTime;
    
    /**
     * Version number, identifying the version information of the knowledge base
     * Format: v1.0.0
     */
    private String version;
    
    /**
     * Status identifier, indicating the current status of the knowledge base
     * 0-disabled, 1-enabled, 2-syncing, 3-error
     */
    private Integer status;
    
    /**
     * Node type, identifying the deployment node of the knowledge base
     * public_cloud-public cloud, edge-edge node
     */
    private String nodeType;
    
    /**
     * Data checksum, used for data integrity verification
     */
    private String checksum;
    
    // Status constants
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_SYNCING = 2;
    public static final int STATUS_ERROR = 3;
    
    // Node type constants
    public static final String NODE_PUBLIC_CLOUD = "public_cloud";
    public static final String NODE_EDGE = "edge";
    
    // Default sync interval (1 hour)
    private static final Duration DEFAULT_SYNC_INTERVAL = Duration.ofHours(1);
    
    // Checksum separator to reduce collision risk
    private static final String CHECKSUM_SEPARATOR = "\u0001"; // Use unprintable character as separator
    
    /**
     * Default constructor
     * Initializes default values and creation time
     */
    public KnowledgeBase() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.status = STATUS_ENABLED;
        this.version = "v1.0.0";
        this.nodeType = NODE_PUBLIC_CLOUD;
        this.checksum = calculateChecksum();
    }
    
    /**
     * Parameterized constructor
     * 
     * @param venueMapData Venue map data
     * @param ruleText Rule text
     * @param vectorIndex Vector index
     * @param nodeType Node type (public_cloud/edge)
     */
    public KnowledgeBase(String venueMapData, String ruleText, String vectorIndex, String nodeType) {
        this();
        this.venueMapData = venueMapData;
        this.ruleText = ruleText;
        this.vectorIndex = vectorIndex;
        this.nodeType = Objects.requireNonNullElse(nodeType, NODE_PUBLIC_CLOUD);
        this.checksum = calculateChecksum();
    }
    
    /**
     * Update knowledge base content and refresh update time and version
     * 
     * @param venueMapData New venue map data
     * @param ruleText New rule text
     * @param vectorIndex New vector index
     * @return Current knowledge base instance
     */
    public KnowledgeBase updateContent(String venueMapData, String ruleText, String vectorIndex) {
        this.venueMapData = venueMapData;
        this.ruleText = ruleText;
        this.vectorIndex = vectorIndex;
        this.updateTime = LocalDateTime.now();
        this.checksum = calculateChecksum();
        incrementVersion();
        return this;
    }
    
    /**
     * Check if knowledge base is in available status
     * 
     * @return true-available, false-unavailable
     */
    public boolean isAvailable() {
        return this.status != null && this.status == STATUS_ENABLED;
    }
    
    /**
     * Check if knowledge base needs synchronization
     * Based on update time judgment, customizable sync interval
     * 
     * @param syncInterval Synchronization interval threshold
     * @return true-needs sync, false-no sync needed
     */
    public boolean needsSync(Duration syncInterval) {
        if (this.updateTime == null) {
            return true;
        }
        Duration interval = syncInterval != null ? syncInterval : DEFAULT_SYNC_INTERVAL;
        LocalDateTime thresholdTime = LocalDateTime.now().minus(interval);
        return this.updateTime.isBefore(thresholdTime);
    }
    
    /**
     * Check if knowledge base needs synchronization with default interval
     * 
     * @return true-needs sync, false-no sync needed
     */
    public boolean needsSync() {
        return needsSync(DEFAULT_SYNC_INTERVAL);
    }
    
    /**
     * Calculate checksum for data integrity verification
     * Uses CRC32 algorithm to calculate checksum of core fields with safe separator
     * 
     * @return Calculated checksum string
     */
    private String calculateChecksum() {
        CRC32 crc32 = new CRC32();
        String dataToCheck = String.join(CHECKSUM_SEPARATOR, 
            Objects.toString(venueMapData, ""),
            Objects.toString(ruleText, ""),
            Objects.toString(vectorIndex, ""),
            Objects.toString(version, ""));
        
        crc32.update(dataToCheck.getBytes());
        return Long.toHexString(crc32.getValue());
    }
    
    /**
     * Verify data integrity by comparing current checksum with stored checksum
     * 
     * @return true-data integrity verified, false-data may be corrupted
     */
    public boolean verifyIntegrity() {
        if (this.checksum == null) {
            return false;
        }
        return this.checksum.equals(calculateChecksum());
    }
    
    /**
     * Increment version number following semantic versioning principles
     * Supports major.minor.patch format with proper validation
     */
    private void incrementVersion() {
        if (this.version == null || !this.version.startsWith("v")) {
            this.version = "v1.0.0";
            return;
        }
        
        try {
            String versionWithoutPrefix = this.version.substring(1);
            String[] parts = versionWithoutPrefix.split("\\.");
            
            if (parts.length < 3) {
                // Invalid format, reset to default
                this.version = "v1.0.0";
                return;
            }
            
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]) + 1; // Increment patch version
            
            // Handle version rollover
            if (patch > 999) {
                patch = 0;
                minor++;
                if (minor > 99) {
                    minor = 0;
                    major++;
                }
            }
            
            this.version = String.format("v%d.%d.%d", major, minor, patch);
            
        } catch (NumberFormatException e) {
            // If version format is invalid, reset to default
            this.version = "v1.0.0";
        }
    }
    
    /**
     * Change knowledge base status with validation
     * 
     * @param newStatus New status to set
     * @return true-status change successful, false-invalid status transition
     */
    public boolean changeStatus(int newStatus) {
        if (!isValidStatusTransition(this.status, newStatus)) {
            return false;
        }
        this.status = newStatus;
        this.updateTime = LocalDateTime.now();
        return true;
    }
    
    /**
     * Validate status transition
     * 
     * @param currentStatus Current status
     * @param newStatus New status to transition to
     * @return true-valid transition, false-invalid transition
     */
    private boolean isValidStatusTransition(Integer currentStatus, int newStatus) {
        if (currentStatus == null) {
            return newStatus == STATUS_ENABLED || newStatus == STATUS_DISABLED;
        }
        
        switch (currentStatus) {
            case STATUS_DISABLED:
                return newStatus == STATUS_ENABLED || newStatus == STATUS_SYNCING;
            case STATUS_ENABLED:
                return newStatus == STATUS_DISABLED || newStatus == STATUS_SYNCING;
            case STATUS_SYNCING:
                return newStatus == STATUS_ENABLED || newStatus == STATUS_ERROR;
            case STATUS_ERROR:
                return newStatus == STATUS_SYNCING || newStatus == STATUS_DISABLED;
            default:
                return false;
        }
    }
    
    /**
     * Check if this is a public cloud node
     * 
     * @return true-public cloud node, false-edge node or other
     */
    public boolean isPublicCloudNode() {
        return NODE_PUBLIC_CLOUD.equals(this.nodeType);
    }
    
    /**
     * Check if this is an edge node
     * 
     * @return true-edge node, false-public cloud node or other
     */
    public boolean isEdgeNode() {
        return NODE_EDGE.equals(this.nodeType);
    }
    
    /**
     * Get sync interval based on node type
     * Edge nodes might need more frequent sync than public cloud nodes
     * 
     * @return Appropriate sync interval for this node type
     */
    public Duration getRecommendedSyncInterval() {
        if (isEdgeNode()) {
            return Duration.ofMinutes(30); // More frequent sync for edge nodes
        }
        return DEFAULT_SYNC_INTERVAL;
    }
}


// 内容由AI生成，仅供参考