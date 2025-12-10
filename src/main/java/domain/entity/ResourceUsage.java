package domain.entity;

import lombok.Data;
import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.Date;

/**
 * Resource usage domain entity class
 * Represents system resource usage for cost optimization and resource scheduling
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Data
@Entity
@Table(name = "resource_usage")
public class ResourceUsage {
    
    /**
     * Primary key ID, auto-increment
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * GPU usage rate, unit: percentage (0-100)
     */
    @Column(name = "gpu_usage_rate", nullable = false, precision = 5, scale = 2)
    @Min(0) @Max(100)
    @NotNull
    private Double gpuUsageRate = 0.0;
    
    /**
     * Memory usage, unit: bytes
     */
    @Column(name = "memory_usage", nullable = false)
    @PositiveOrZero
    @NotNull
    private Long memoryUsage = 0L;
    
    /**
     * Network traffic, unit: bytes
     */
    @Column(name = "network_traffic", nullable = false)
    @PositiveOrZero
    @NotNull
    private Long networkTraffic = 0L;
    
    /**
     * Cost statistics, unit: RMB
     */
    @Column(name = "cost_statistics", nullable = false, precision = 10, scale = 2)
    @PositiveOrZero
    @NotNull
    private Double costStatistics = 0.0;
    
    /**
     * Record timestamp
     */
    @Column(name = "record_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date recordTime;
    
    /**
     * Tenant ID for multi-tenant resource isolation
     */
    @Column(name = "tenant_id", nullable = false, length = 50)
    @NotNull
    private String tenantId;
    
    /**
     * Resource type identifier (e.g., inference service, training task)
     */
    @Column(name = "resource_type", nullable = false, length = 50)
    @NotNull
    private String resourceType;
    
    /**
     * Node identifier, records the specific node where resources are used
     */
    @Column(name = "node_identifier", nullable = false, length = 100)
    @NotNull
    private String nodeIdentifier;
    
    /**
     * Default constructor
     */
    public ResourceUsage() {
        this.recordTime = new Date();
        this.tenantId = "default";
        this.resourceType = "general";
        this.nodeIdentifier = "unknown";
    }
    
    /**
     * Parameterized constructor with all fields
     * 
     * @param gpuUsageRate GPU usage rate
     * @param memoryUsage Memory usage
     * @param networkTraffic Network traffic
     * @param costStatistics Cost statistics
     * @param tenantId Tenant ID
     * @param resourceType Resource type
     * @param nodeIdentifier Node identifier
     * @param recordTime Record time
     */
    public ResourceUsage(Double gpuUsageRate, Long memoryUsage, Long networkTraffic, 
                        Double costStatistics, String tenantId, String resourceType, 
                        String nodeIdentifier, Date recordTime) {
        this.gpuUsageRate = gpuUsageRate != null ? gpuUsageRate : 0.0;
        this.memoryUsage = memoryUsage != null ? memoryUsage : 0L;
        this.networkTraffic = networkTraffic != null ? networkTraffic : 0L;
        this.costStatistics = costStatistics != null ? costStatistics : 0.0;
        this.tenantId = tenantId != null ? tenantId : "default";
        this.resourceType = resourceType != null ? resourceType : "general";
        this.nodeIdentifier = nodeIdentifier != null ? nodeIdentifier : "unknown";
        this.recordTime = recordTime != null ? recordTime : new Date();
    }
    
    /**
     * Parameterized constructor without recordTime (uses current time)
     */
    public ResourceUsage(Double gpuUsageRate, Long memoryUsage, Long networkTraffic, 
                        Double costStatistics, String tenantId, String resourceType, 
                        String nodeIdentifier) {
        this(gpuUsageRate, memoryUsage, networkTraffic, costStatistics, 
             tenantId, resourceType, nodeIdentifier, new Date());
    }
    
    /**
     * Checks if current resource usage exceeds specified thresholds
     * 
     * @param gpuThreshold GPU usage rate threshold
     * @param memoryThreshold Memory usage threshold
     * @param networkThreshold Network traffic threshold
     * @param costThreshold Cost statistics threshold
     * @return true if any threshold is exceeded
     * @throws IllegalArgumentException if any threshold is null
     */
    public boolean exceedsThreshold(Double gpuThreshold, Long memoryThreshold, 
                                   Long networkThreshold, Double costThreshold) {
        if (gpuThreshold == null || memoryThreshold == null || 
            networkThreshold == null || costThreshold == null) {
            throw new IllegalArgumentException("All thresholds must be provided");
        }
        
        return (this.gpuUsageRate > gpuThreshold) || 
               (this.memoryUsage > memoryThreshold) || 
               (this.networkTraffic > networkThreshold) || 
               (this.costStatistics > costThreshold);
    }
    
    /**
     * Calculates the cost ratio of this resource usage compared to total cost
     * 
     * @param totalCost Total cost for comparison
     * @return Cost ratio (0-1)
     * @throws IllegalArgumentException if totalCost is null or not positive
     */
    public Double calculateCostRatio(Double totalCost) {
        if (totalCost == null) {
            throw new IllegalArgumentException("Total cost cannot be null");
        }
        if (totalCost <= 0) {
            throw new IllegalArgumentException("Total cost must be positive");
        }
        return this.costStatistics / totalCost;
    }
    
    /**
     * Calculates resource utilization score based on weighted factors
     * 
     * @param gpuWeight Weight for GPU usage (default 0.4)
     * @param memoryWeight Weight for memory usage (default 0.3)
     * @param networkWeight Weight for network usage (default 0.2)
     * @param costWeight Weight for cost (default 0.1)
     * @param maxMemory Maximum memory for normalization (default 1GB)
     * @param maxNetwork Maximum network for normalization (default 1MB)
     * @param maxCost Maximum cost for normalization (default 1000元)
     * @return Utilization score (0-100)
     */
    public Double calculateUtilizationScore(Double gpuWeight, Double memoryWeight, 
                                           Double networkWeight, Double costWeight,
                                           Long maxMemory, Long maxNetwork, Double maxCost) {
        double gpuW = gpuWeight != null ? gpuWeight : 0.4;
        double memoryW = memoryWeight != null ? memoryWeight : 0.3;
        double networkW = networkWeight != null ? networkWeight : 0.2;
        double costW = costWeight != null ? costWeight : 0.1;
        
        // Normalize weights to sum to 1.0
        double totalWeight = gpuW + memoryW + networkW + costW;
        gpuW /= totalWeight;
        memoryW /= totalWeight;
        networkW /= totalWeight;
        costW /= totalWeight;
        
        // Use provided maximum values or defaults for normalization
        long normMaxMemory = maxMemory != null ? maxMemory : 1024L * 1024 * 1024; // 1GB
        long normMaxNetwork = maxNetwork != null ? maxNetwork : 1024L * 1024; // 1MB
        double normMaxCost = maxCost != null ? maxCost : 1000.0; // 1000元
        
        // Calculate normalized scores with safety checks
        double gpuScore = Math.min(this.gpuUsageRate / 100.0, 1.0);
        double memoryScore = normMaxMemory > 0 ? Math.min((double) this.memoryUsage / normMaxMemory, 1.0) : 0.0;
        double networkScore = normMaxNetwork > 0 ? Math.min((double) this.networkTraffic / normMaxNetwork, 1.0) : 0.0;
        double costScore = normMaxCost > 0 ? Math.min(this.costStatistics / normMaxCost, 1.0) : 0.0;
        
        return (gpuScore * gpuW + memoryScore * memoryW + 
                networkScore * networkW + costScore * costW) * 100;
    }
    
    /**
     * Overloaded method with default normalization values
     */
    public Double calculateUtilizationScore(Double gpuWeight, Double memoryWeight, 
                                           Double networkWeight, Double costWeight) {
        return calculateUtilizationScore(gpuWeight, memoryWeight, networkWeight, costWeight, 
                                        null, null, null);
    }
    
    /**
     * Returns formatted resource usage information
     * 
     * @return Formatted resource usage information
     */
    @Override
    public String toString() {
        return String.format(
            "ResourceUsage{id=%d, gpuUsageRate=%.2f%%, memoryUsage=%d bytes, " +
            "networkTraffic=%d bytes, costStatistics=%.2f元, recordTime=%s, " +
            "tenantId='%s', resourceType='%s', nodeIdentifier='%s'}",
            id, gpuUsageRate, memoryUsage, networkTraffic, costStatistics, 
            recordTime, tenantId, resourceType, nodeIdentifier
        );
    }
    
    /**
     * Returns a brief summary of resource usage with safe null handling
     * 
     * @return Brief summary string
     */
    public String toBriefString() {
        long memoryMb = memoryUsage != null ? memoryUsage / (1024 * 1024) : 0;
        long networkMb = networkTraffic != null ? networkTraffic / (1024 * 1024) : 0;
        
        return String.format(
            "GPU: %.1f%%, Memory: %d MB, Network: %d MB, Cost: ¥%.2f",
            gpuUsageRate != null ? gpuUsageRate : 0.0,
            memoryMb,
            networkMb,
            costStatistics != null ? costStatistics : 0.0
        );
    }
}


// 内容由AI生成，仅供参考