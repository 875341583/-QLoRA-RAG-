package com.navigation.adapter.controller;

import com.navigation.application.service.CostOptimizationService;
import com.navigation.domain.entity.ResourceUsage;
import com.navigation.adapter.dto.ThresholdConfigDTO;
import com.navigation.adapter.dto.TenantConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.util.Map;

/**
 * 成本优化与资源调度控制器
 * 负责处理资源监控、动态分配、成本分析等HTTP请求
 * 
 * @author Alex
 * @version 1.0
 */
@Tag(name = "成本优化管理", description = "成本优化与资源调度相关API")
@Validated
@RestController
@RequestMapping("/api/cost-optimization")
public class CostOptimizationController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CostOptimizationController.class);
    private static final long MAX_RESPONSE_TIME_MS = 800L;
    private static final String MONTH_PATTERN = "^\\d{4}-(0[1-9]|1[0-2])$";
    
    private final CostOptimizationService costOptimizationService;
    
    /**
     * 构造函数注入服务依赖
     * @param costOptimizationService 成本优化服务
     */
    @Autowired
    public CostOptimizationController(CostOptimizationService costOptimizationService) {
        this.costOptimizationService = costOptimizationService;
    }
    
    /**
     * 监控GPU资源使用情况
     * @return GPU使用率统计信息
     */
    @Operation(summary = "监控GPU资源使用", description = "实时获取GPU资源使用情况统计")
    @GetMapping("/monitor-gpu-usage")
    public ResponseEntity<Map<String, Object>> monitorGPUUsage() {
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> gpuUsage = costOptimizationService.monitorGPUUsage();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 记录性能指标，实际时延监控应在服务层实现
            if (responseTime > MAX_RESPONSE_TIME_MS) {
                LOGGER.warn("GPU监控接口响应时间较长: {}ms", responseTime);
            }
            
            LOGGER.debug("GPU监控接口调用成功，响应时间: {}ms", responseTime);
            return ResponseEntity.ok(gpuUsage);
        } catch (Exception e) {
            LOGGER.error("GPU监控接口调用异常", e);
            throw e; // 由全局异常处理器统一处理
        }
    }
    
    /**
     * 根据请求峰值动态分配资源
     * @param peakRequestCount 峰值请求数量
     * @return 资源分配结果
     */
    @Operation(summary = "动态资源分配", description = "根据请求峰值动态调整资源分配策略")
    @PostMapping("/dynamic-resource-allocation")
    public ResponseEntity<String> dynamicResourceAllocation(
            @Parameter(description = "峰值请求数量", required = true, example = "1000")
            @RequestParam("peakRequestCount") 
            @Min(value = 1, message = "峰值请求数量必须大于0") int peakRequestCount) {
        
        LOGGER.info("接收到动态资源分配请求，峰值请求数: {}", peakRequestCount);
        String allocationResult = costOptimizationService.dynamicResourceAllocation(peakRequestCount);
        return ResponseEntity.ok(allocationResult);
    }
    
    /**
     * 生成月度成本分析报告
     * @param month 月份，格式：yyyy-MM
     * @return 成本分析报告数据
     */
    @Operation(summary = "生成月度成本报告", description = "生成指定月份的详细成本分析报告")
    @GetMapping("/generate-monthly-report")
    public ResponseEntity<ResourceUsage> generateMonthlyReport(
            @Parameter(description = "月份，格式：yyyy-MM", required = true, example = "2024-01")
            @RequestParam("month") 
            @Pattern(regexp = MONTH_PATTERN, message = "月份格式必须为yyyy-MM") String month) {
        
        LOGGER.info("生成月度成本报告，月份: {}", month);
        ResourceUsage monthlyReport = costOptimizationService.generateMonthlyReport(month);
        return ResponseEntity.ok(monthlyReport);
    }
    
    /**
     * 设置资源使用阈值并触发告警
     * @param thresholdConfig 阈值配置参数
     * @return 设置结果
     */
    @Operation(summary = "设置资源阈值", description = "配置资源使用阈值并设置告警规则")
    @PostMapping("/set-resource-thresholds")
    public ResponseEntity<String> setResourceThresholds(
            @Parameter(description = "阈值配置参数", required = true)
            @Valid @RequestBody ThresholdConfigDTO thresholdConfig) {
        
        LOGGER.info("设置资源阈值配置: {}", thresholdConfig);
        String result = costOptimizationService.setResourceThresholds(thresholdConfig.toMap());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 实现多租户资源隔离
     * @param tenantConfig 租户配置信息
     * @return 隔离策略执行结果
     */
    @Operation(summary = "多租户资源隔离", description = "实现多租户间的资源隔离和配额管理")
    @PostMapping("/implement-multi-tenant-isolation")
    public ResponseEntity<String> implementMultiTenantIsolation(
            @Parameter(description = "租户配置信息", required = true)
            @Valid @RequestBody TenantConfigDTO tenantConfig) {
        
        LOGGER.info("配置多租户资源隔离: {}", tenantConfig);
        String isolationResult = costOptimizationService.implementMultiTenantIsolation(tenantConfig.toMap());
        return ResponseEntity.ok(isolationResult);
    }
    
    /**
     * 获取当前资源使用状态（扩展功能）
     * @return 资源使用状态概览
     */
    @Operation(summary = "获取资源状态", description = "获取系统当前资源使用状态概览")
    @GetMapping("/resource-status")
    public ResponseEntity<Map<String, Object>> getResourceStatus() {
        LOGGER.debug("获取当前资源状态");
        Map<String, Object> status = costOptimizationService.getCurrentResourceStatus();
        return ResponseEntity.ok(status);
    }
}


// 内容由AI生成，仅供参考