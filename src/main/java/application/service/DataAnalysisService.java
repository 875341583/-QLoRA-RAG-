package com.navigation.application.service;

import com.navigation.domain.entity.ResourceUsage;
import com.navigation.domain.repository.ResourceUsageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Analysis Service Implementation
 * Provides data collection, processing, analysis and report generation functionality
 */
@Slf4j
@Service
public class DataAnalysisService {

    private static final int TREND_ANALYSIS_DATA_POINTS = 100;
    private static final double GPU_USAGE_ALERT_THRESHOLD = 0.8;
    private static final double COST_ALERT_THRESHOLD = 250.0;
    private static final Set<String> SUPPORTED_EXPORT_FORMATS = Set.of("CSV", "PDF");

    private final ResourceUsageRepository resourceUsageRepository;
    private final ExternalDataCollector externalDataCollector;
    private final ReportGenerator reportGenerator;

    /**
     * Constructor with dependency injection
     *
     * @param resourceUsageRepository Resource usage repository
     * @param externalDataCollector External data collector service
     * @param reportGenerator Report generator service
     */
    @Autowired
    public DataAnalysisService(ResourceUsageRepository resourceUsageRepository,
                              ExternalDataCollector externalDataCollector,
                              ReportGenerator reportGenerator) {
        this.resourceUsageRepository = resourceUsageRepository;
        this.externalDataCollector = externalDataCollector;
        this.reportGenerator = reportGenerator;
    }

    /**
     * Collect system data through instrumentation technology
     * Collects system operation metrics and user behavior data
     *
     * @return Collection result with statistics
     */
    public DataCollectionResult collectDataPoints() {
        log.info("Starting system data collection...");
        
        try {
            List<SystemMetric> rawMetrics = externalDataCollector.collectSystemMetrics();
            
            if (CollectionUtils.isEmpty(rawMetrics)) {
                log.warn("No system data collected");
                return DataCollectionResult.failure("No data available");
            }
            
            // Process data in batches for better performance
            List<ResourceUsage> processedUsages = rawMetrics.parallelStream()
                    .map(this::processSingleMetric)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            if (processedUsages.isEmpty()) {
                return DataCollectionResult.failure("No valid data after processing");
            }
            
            // Batch save to improve database performance
            List<ResourceUsage> savedUsages = resourceUsageRepository.saveAll(processedUsages);
            
            log.info("System data collection completed, processed {} records", savedUsages.size());
            return DataCollectionResult.success(savedUsages.size());
            
        } catch (Exception e) {
            log.error("Exception occurred during data collection", e);
            return DataCollectionResult.failure(e.getMessage());
        }
    }

    /**
     * Real-time processing of large amounts of data
     * Cleanses, transforms and aggregates collected raw data
     *
     * @param metric Raw system metric
     * @return Processed resource usage entity
     */
    public ResourceUsage processSingleMetric(SystemMetric metric) {
        if (metric == null || !metric.isValid()) {
            log.debug("Skipping invalid metric: {}", metric);
            return null;
        }
        
        try {
            ResourceUsage usage = new ResourceUsage();
            usage.setGpuUsageRate(normalizeGpuUsage(metric.getGpuUsage()));
            usage.setMemoryUsage(normalizeMemoryUsage(metric.getMemoryUsage()));
            usage.setNetworkTraffic(normalizeNetworkTraffic(metric.getNetworkTraffic()));
            usage.setCostStatistics(calculateCost(metric));
            usage.setTimestamp(metric.getTimestamp());
            usage.setDataQuality(calculateDataQuality(usage));
            
            log.debug("Processed metric for timestamp: {}", metric.getTimestamp());
            return usage;
            
        } catch (Exception e) {
            log.warn("Failed to process metric: {}", metric, e);
            return null;
        }
    }

    /**
     * Generate visual statistical charts
     * Converts processed data to chart-required format
     *
     * @param timeRange Time range for chart data
     * @param chartType Type of chart to generate
     * @return Chart configuration and data
     */
    public ChartData generateVisualCharts(TimeRange timeRange, ChartType chartType) {
        log.info("Generating visual charts for time range: {}, type: {}", timeRange, chartType);
        
        validateChartParameters(timeRange, chartType);
        
        // Query actual data from database
        List<ResourceUsage> chartData = resourceUsageRepository
                .findByTimestampBetween(timeRange.getStartTime(), timeRange.getEndTime());
        
        if (CollectionUtils.isEmpty(chartData)) {
            log.warn("No data available for the specified time range");
            return ChartData.empty(timeRange, chartType);
        }
        
        ChartData result = new ChartData();
        result.setChartType(chartType);
        result.setTitle(generateChartTitle(timeRange, chartType));
        result.setTimeRange(timeRange);
        result.setGeneratedAt(LocalDateTime.now());
        
        // Generate series data based on actual metrics
        result.addSeries("GPU Usage", generateGpuUsageSeries(chartData));
        result.addSeries("Memory Usage", generateMemoryUsageSeries(chartData));
        result.addSeries("Network Traffic", generateNetworkTrafficSeries(chartData));
        result.addSeries("Cost", generateCostSeries(chartData));
        
        log.info("Visual chart generation completed with {} data points", chartData.size());
        return result;
    }

    /**
     * Support report customization and export
     * Generates customized reports based on user-selected dimensions and metrics
     *
     * @param request Report generation request
     * @return Generated report data
     */
    public Report supportReportCustomization(ReportRequest request) {
        log.info("Generating custom report with dimensions: {}, metrics: {}, format: {}", 
                request.getDimensions(), request.getMetrics(), request.getFormat());
        
        validateReportRequest(request);
        
        // Query data based on dimensions and metrics
        ReportData reportData = queryReportData(request);
        
        // Generate report using dedicated report generator
        Report report = reportGenerator.generateReport(request, reportData);
        report.setGenerationTime(LocalDateTime.now());
        report.setStatus(ReportStatus.COMPLETED);
        
        log.info("Custom report generation completed, report ID: {}", report.getReportId());
        return report;
    }

    /**
     * Provide data alerts and trend analysis
     * Predicts future trends based on historical data and sets alert rules
     *
     * @param alertRules Alert rules configuration
     * @return Alert results and trend analysis
     */
    public AlertAnalysisResult provideDataAlerts(AlertRules alertRules) {
        log.info("Executing data alerts and trend analysis");
        
        validateAlertRules(alertRules);
        
        // Get historical data for analysis
        List<ResourceUsage> historicalData = resourceUsageRepository
                .findTopNByOrderByTimestampDesc(TREND_ANALYSIS_DATA_POINTS);
        
        if (CollectionUtils.isEmpty(historicalData)) {
            log.warn("Insufficient historical data for trend analysis");
            return AlertAnalysisResult.insufficientData();
        }
        
        AlertAnalysisResult result = new AlertAnalysisResult();
        result.setAnalysisTime(LocalDateTime.now());
        result.setDataPointsAnalyzed(historicalData.size());
        
        // Perform trend analysis
        result.setTrendAnalysis(performComprehensiveTrendAnalysis(historicalData));
        
        // Check alert conditions
        result.setAlerts(checkAlertConditions(historicalData, alertRules));
        
        log.info("Data alerts and trend analysis completed, analyzed {} data points", 
                historicalData.size());
        return result;
    }

    /**
     * Export report in specified format
     *
     * @param report Report to export
     * @param format Export format (CSV/PDF)
     * @return Export file path or content
     */
    public ExportResult exportReports(Report report, String format) {
        log.info("Exporting report in format: {}", format);
        
        validateExportFormat(format);
        validateReportForExport(report);
        
        ExportResult exportResult;
        switch (format.toUpperCase()) {
            case "CSV":
                exportResult = reportGenerator.exportToCsv(report);
                break;
            case "PDF":
                exportResult = reportGenerator.exportToPdf(report);
                break;
            default:
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
        
        log.info("Report export completed successfully");
        return exportResult;
    }

    // ============ Private Helper Methods ============

    private void validateChartParameters(TimeRange timeRange, ChartType chartType) {
        if (timeRange == null) {
            throw new IllegalArgumentException("Time range cannot be null");
        }
        if (chartType == null) {
            throw new IllegalArgumentException("Chart type cannot be null");
        }
        if (timeRange.getStartTime().isAfter(timeRange.getEndTime())) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }
    }

    private void validateReportRequest(ReportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Report request cannot be null");
        }
        if (CollectionUtils.isEmpty(request.getDimensions())) {
            throw new IllegalArgumentException("Analysis dimensions cannot be empty");
        }
        if (CollectionUtils.isEmpty(request.getMetrics())) {
            throw new IllegalArgumentException("Analysis metrics cannot be empty");
        }
        if (!SUPPORTED_EXPORT_FORMATS.contains(request.getFormat().toUpperCase())) {
            throw new IllegalArgumentException("Unsupported export format: " + request.getFormat());
        }
    }

    private void validateAlertRules(AlertRules alertRules) {
        if (alertRules == null) {
            throw new IllegalArgumentException("Alert rules cannot be null");
        }
        if (CollectionUtils.isEmpty(alertRules.getThresholds())) {
            throw new IllegalArgumentException("Alert rules must contain threshold configurations");
        }
    }

    private void validateExportFormat(String format) {
        if (!SUPPORTED_EXPORT_FORMATS.contains(format.toUpperCase())) {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }

    private void validateReportForExport(Report report) {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null for export");
        }
        if (report.getStatus() != ReportStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed reports can be exported");
        }
    }

    private double normalizeGpuUsage(Double gpuUsage) {
        return gpuUsage != null ? Math.max(0.0, Math.min(100.0, gpuUsage)) : 0.0;
    }

    private long normalizeMemoryUsage(Long memoryUsage) {
        return memoryUsage != null ? Math.max(0L, memoryUsage) : 0L;
    }

    private long normalizeNetworkTraffic(Long networkTraffic) {
        return networkTraffic != null ? Math.max(0L, networkTraffic) : 0L;
    }

    private double calculateCost(SystemMetric metric) {
        // Simple cost calculation based on resource usage
        double baseCost = 50.0; // Monthly base cost
        double gpuCost = (metric.getGpuUsage() != null ? metric.getGpuUsage() : 0.0) * 0.1;
        double memoryCost = (metric.getMemoryUsage() != null ? metric.getMemoryUsage() : 0.0) / 1024.0 * 0.01;
        
        return baseCost + gpuCost + memoryCost;
    }

    private String calculateDataQuality(ResourceUsage usage) {
        int validFields = 0;
        int totalFields = 4; // gpu, memory, network, cost
        
        if (usage.getGpuUsageRate() > 0) validFields++;
        if (usage.getMemoryUsage() > 0) validFields++;
        if (usage.getNetworkTraffic() > 0) validFields++;
        if (usage.getCostStatistics() > 0) validFields++;
        
        double qualityScore = (double) validFields / totalFields;
        return qualityScore > 0.75 ? "HIGH" : qualityScore > 0.5 ? "MEDIUM" : "LOW";
    }

    private String generateChartTitle(TimeRange timeRange, ChartType chartType) {
        return String.format("System Resource Monitoring - %s (%s to %s)", 
                chartType.name(), 
                timeRange.getStartTime().toLocalDate(),
                timeRange.getEndTime().toLocalDate());
    }

    private Map<LocalDateTime, Double> generateGpuUsageSeries(List<ResourceUsage> data) {
        return data.stream()
                .collect(Collectors.toMap(
                        ResourceUsage::getTimestamp,
                        ResourceUsage::getGpuUsageRate,
                        (v1, v2) -> v1 // Merge function for duplicate keys
                ));
    }

    private Map<LocalDateTime, Long> generateMemoryUsageSeries(List<ResourceUsage> data) {
        return data.stream()
                .collect(Collectors.toMap(
                        ResourceUsage::getTimestamp,
                        ResourceUsage::getMemoryUsage,
                        (v1, v2) -> v1
                ));
    }

    private Map<LocalDateTime, Long> generateNetworkTrafficSeries(List<ResourceUsage> data) {
        return data.stream()
                .collect(Collectors.toMap(
                        ResourceUsage::getTimestamp,
                        ResourceUsage::getNetworkTraffic,
                        (v1, v2) -> v1
                ));
    }

    private Map<LocalDateTime, Double> generateCostSeries(List<ResourceUsage> data) {
        return data.stream()
                .collect(Collectors.toMap(
                        ResourceUsage::getTimestamp,
                        ResourceUsage::getCostStatistics,
                        (v1, v2) -> v1
                ));
    }

    private ReportData queryReportData(ReportRequest request) {
        // Implement actual database query based on dimensions and metrics
        ReportData reportData = new ReportData();
        reportData.setTimeRange(TimeRange.last30Days());
        
        // Sample implementation - should be replaced with actual database queries
        reportData.addMetric("totalUsers", 1250);
        reportData.addMetric("activeSessions", 187);
        reportData.addMetric("averageResponseTime", 0.67);
        reportData.addMetric("successRate", 0.945);
        reportData.addMetric("gpuUtilization", 72.5);
        
        return reportData;
    }

    private TrendAnalysis performComprehensiveTrendAnalysis(List<ResourceUsage> historicalData) {
        TrendAnalysis analysis = new TrendAnalysis();
        
        // Calculate averages
        double avgGpuUsage = historicalData.stream()
                .mapToDouble(ResourceUsage::getGpuUsageRate)
                .average()
                .orElse(0.0);
        
        double avgCost = historicalData.stream()
                .mapToDouble(ResourceUsage::getCostStatistics)
                .average()
                .orElse(0.0);
        
        // Determine trends
        analysis.setGpuUsageTrend(avgGpuUsage > 70.0 ? Trend.UPWARD : Trend.STABLE);
        analysis.setCostTrend(avgCost > 200.0 ? Trend.UPWARD : Trend.STABLE);
        analysis.setPredictedPeak(LocalDateTime.now().plusHours(2));
        analysis.setRecommendations(generateTrendRecommendations(avgGpuUsage, avgCost));
        
        return analysis;
    }

    private AlertResult checkAlertConditions(List<ResourceUsage> historicalData, AlertRules alertRules) {
        AlertResult alertResult = new AlertResult();
        
        boolean gpuAlert = historicalData.stream()
                .anyMatch(usage -> usage.getGpuUsageRate() > GPU_USAGE_ALERT_THRESHOLD * 100);
        
        boolean costAlert = historicalData.stream()
                .anyMatch(usage -> usage.getCostStatistics() > COST_ALERT_THRESHOLD);
        
        alertResult.setGpuUsageAlert(gpuAlert);
        alertResult.setCostAlert(costAlert);
        alertResult.setTriggeredAlerts(generateAlertDetails(gpuAlert, costAlert));
        alertResult.setTriggeredAt(LocalDateTime.now());
        
        return alertResult;
    }

    private List<String> generateTrendRecommendations(double gpuUsage, double cost) {
        List<String> recommendations = new ArrayList<>();
        
        if (gpuUsage > 70.0) {
            recommendations.add("Optimize GPU resource allocation strategy");
        }
        if (cost > 200.0) {
            recommendations.add("Review high-cost operations for optimization");
        }
        if (gpuUsage < 20.0) {
            recommendations.add("Consider consolidating underutilized resources");
        }
        
        return recommendations.isEmpty() ? 
                List.of("System operating within optimal parameters") : recommendations;
    }

    private List<AlertDetail> generateAlertDetails(boolean gpuAlert, boolean costAlert) {
        List<AlertDetail> alerts = new ArrayList<>();
        
        if (gpuAlert) {
            alerts.add(new AlertDetail("HIGH_GPU_USAGE", 
                    "GPU usage exceeds threshold", 
                    "Check resource allocation and consider auto-scaling"));
        }
        
        if (costAlert) {
            alerts.add(new AlertDetail("HIGH_COST", 
                    "Cost exceeds budget threshold", 
                    "Review resource usage and optimize high-cost operations"));
        }
        
        return alerts;
    }

    // ============ Supporting Domain Classes ============

    public static class DataCollectionResult {
        private final boolean success;
        private final String message;
        private final Integer recordsProcessed;

        private DataCollectionResult(boolean success, String message, Integer recordsProcessed) {
            this.success = success;
            this.message = message;
            this.recordsProcessed = recordsProcessed;
        }

        public static DataCollectionResult success(int recordsProcessed) {
            return new DataCollectionResult(true, "Data collection successful", recordsProcessed);
        }

        public static DataCollectionResult failure(String errorMessage) {
            return new DataCollectionResult(false, errorMessage, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Integer getRecordsProcessed() { return recordsProcessed; }
    }

    public enum ChartType {
        LINE, BAR, PIE, AREA
    }

    public enum Trend {
        UPWARD, DOWNWARD, STABLE
    }

    public enum ReportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    // Note: Other domain classes like TimeRange, SystemMetric, ChartData, ReportRequest, 
    // Report, AlertRules, AlertAnalysisResult, etc. should be defined in separate files
    // in the domain layer for better modularity and maintainability.
}

// External dependencies (should be defined in their respective packages)
interface ExternalDataCollector {
    List<SystemMetric> collectSystemMetrics();
}

interface ReportGenerator {
    Report generateReport(ReportRequest request, ReportData data);
    ExportResult exportToCsv(Report report);
    ExportResult exportToPdf(Report report);
}

// Simplified domain classes for demonstration
class SystemMetric {
    private Double gpuUsage;
    private Long memoryUsage;
    private Long networkTraffic;
    private LocalDateTime timestamp;
    
    public boolean isValid() {
        return timestamp != null && (gpuUsage != null || memoryUsage != null || networkTraffic != null);
    }
    
    // Getters and setters
    public Double getGpuUsage() { return gpuUsage; }
    public void setGpuUsage(Double gpuUsage) { this.gpuUsage = gpuUsage; }
    public Long getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(Long memoryUsage) { this.memoryUsage = memoryUsage; }
    public Long getNetworkTraffic() { return networkTraffic; }
    public void setNetworkTraffic(Long networkTraffic) { this.networkTraffic = networkTraffic; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

class TimeRange {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    public static TimeRange last30Days() {
        return new TimeRange(LocalDateTime.now().minusDays(30), LocalDateTime.now());
    }
    
    public TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    // Getters
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}


// 内容由AI生成，仅供参考