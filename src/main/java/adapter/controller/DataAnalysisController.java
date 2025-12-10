package adapter.controller;

import adapter.dto.CustomAnalysisRequest;
import application.service.DataAnalysisService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Data Analysis Controller
 * Handles HTTP requests for data collection, statistical analysis, and report generation.
 * 
 * @author Alex
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/data-analysis")
@Api(tags = "Data Analysis Management")
public class DataAnalysisController {
    
    private final DataAnalysisService dataAnalysisService;

    /**
     * Constructor with dependency injection.
     * 
     * @param dataAnalysisService data analysis service instance
     */
    public DataAnalysisController(DataAnalysisService dataAnalysisService) {
        this.dataAnalysisService = dataAnalysisService;
    }

    /**
     * Collect system operation data and user behavior data.
     * 
     * @return ResponseEntity containing data collection result
     */
    @PostMapping("/collect-system-data")
    @ApiOperation("Collect System Operation Data")
    public ResponseEntity<String> collectSystemData() {
        try {
            log.info("Starting to collect system operation data and user behavior data");
            dataAnalysisService.collectDataPoints();
            log.info("System data collection completed successfully");
            return ResponseEntity.ok("Data collection successful");
        } catch (Exception e) {
            log.error("Exception occurred while collecting system data", e);
            return ResponseEntity.internalServerError().body("Data collection failed: " + e.getMessage());
        }
    }

    /**
     * Generate multi-dimensional statistical reports.
     * 
     * @param dimensions analysis dimensions list
     * @param metrics statistical metrics list
     * @param startTime start time
     * @param endTime end time
     * @return ResponseEntity containing statistical report data
     */
    @GetMapping("/generate-statistical-reports")
    @ApiOperation("Generate Statistical Reports")
    public ResponseEntity<Map<String, Object>> generateStatisticalReports(
            @RequestParam List<String> dimensions,
            @RequestParam List<String> metrics,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        try {
            log.info("Starting to generate statistical reports, dimensions: {}, metrics: {}, time range: {} - {}", 
                    dimensions, metrics, startTime, endTime);
            
            Map<String, Object> report = dataAnalysisService.generateStatisticalReports(
                    dimensions, metrics, startTime, endTime);
            
            log.info("Statistical report generated successfully, containing {} data items", report.size());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Exception occurred while generating statistical reports", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Support custom analysis dimensions and metrics.
     * 
     * @param request custom configuration parameters wrapped in request object
     * @return ResponseEntity containing custom analysis results
     */
    @PostMapping("/support-custom-analysis")
    @ApiOperation("Custom Analysis")
    public ResponseEntity<Map<String, Object>> supportCustomAnalysis(
            @RequestBody CustomAnalysisRequest request) {
        try {
            log.info("Starting custom analysis with configuration parameters: {}", request.getConfigParams());
            
            Map<String, Object> analysisResult = dataAnalysisService.supportCustomAnalysis(request.getConfigParams());
            
            log.info("Custom analysis completed, returning {} analysis results", analysisResult.size());
            return ResponseEntity.ok(analysisResult);
        } catch (Exception e) {
            log.error("Exception occurred while performing custom analysis", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export reports in CSV or PDF format.
     * 
     * @param reportId report ID
     * @param format export format (CSV/PDF)
     * @return ResponseEntity containing exported file data
     */
    @GetMapping("/export-reports")
    @ApiOperation("Export Reports")
    public ResponseEntity<byte[]> exportReports(
            @RequestParam String reportId,
            @RequestParam(defaultValue = "CSV") String format) {
        try {
            log.info("Starting report export, report ID: {}, format: {}", reportId, format);
            
            byte[] exportedData = dataAnalysisService.exportReports(reportId, format.toUpperCase());
            
            String contentType = "CSV".equalsIgnoreCase(format) ? 
                    "text/csv" : "application/pdf";
            String fileName = "report_" + reportId + "." + format.toLowerCase();
            
            log.info("Report exported successfully, file size: {} bytes", exportedData.length);
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(exportedData);
        } catch (Exception e) {
            log.error("Exception occurred while exporting reports", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Set data alert rules.
     * 
     * @param alertRule alert rule configuration
     * @return ResponseEntity containing alert setting result
     */
    @PostMapping("/set-data-alerts")
    @ApiOperation("Set Data Alerts")
    public ResponseEntity<String> setDataAlerts(@RequestParam Map<String, Object> alertRule) {
        try {
            log.info("Setting data alert rules: {}", alertRule);
            
            dataAnalysisService.setDataAlerts(alertRule);
            
            log.info("Data alert rules set successfully");
            return ResponseEntity.ok("Alert rules set successfully");
        } catch (Exception e) {
            log.error("Exception occurred while setting data alert rules", e);
            return ResponseEntity.internalServerError().body("Alert rule setting failed: " + e.getMessage());
        }
    }
}


// 内容由AI生成，仅供参考