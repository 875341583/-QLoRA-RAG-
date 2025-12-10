package adapter.controller;

import application.service.RAGKnowledgeService;
import domain.entity.KnowledgeBase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 混合云RAG架构管理控制器
 * 负责处理知识库更新、状态查询、节点监控等RESTful API请求
 */
@Slf4j
@RestController
@RequestMapping("/api/rag-knowledge")
@Api(tags = "RAG知识库管理接口")
public class RAGKnowledgeController {

    private final RAGKnowledgeService ragKnowledgeService;

    /**
     * 构造函数注入服务依赖
     *
     * @param ragKnowledgeService RAG知识库服务实例
     */
    @Autowired
    public RAGKnowledgeController(RAGKnowledgeService ragKnowledgeService) {
        this.ragKnowledgeService = ragKnowledgeService;
    }

    /**
     * 触发知识库更新
     * 接收管理员上传的场地变更媒体文件，启动知识库更新流程
     *
     * @param mediaFile 场地变更媒体文件（JPEG、PNG、MP4等格式）
     * @return ResponseEntity 包含更新任务ID和状态信息
     */
    @PostMapping("/trigger-update")
    @ApiOperation(value = "触发知识库更新", notes = "接收媒体文件并启动知识库更新流程")
    public ResponseEntity<Map<String, Object>> triggerKnowledgeUpdate(
            @RequestParam("mediaFile") MultipartFile mediaFile) {
        log.info("接收到知识库更新请求，文件名: {}", mediaFile.getOriginalFilename());
        
        try {
            String updateId = ragKnowledgeService.triggerKnowledgeUpdate(mediaFile);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "updateId", updateId,
                "message", "知识库更新任务已启动"
            ));
        } catch (Exception e) {
            log.error("知识库更新请求处理失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "知识库更新失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 查询知识库更新状态
     * 根据更新任务ID获取当前更新进度和状态
     *
     * @param updateId 更新任务唯一标识
     * @return ResponseEntity 包含更新状态和进度信息
     */
    @GetMapping("/update-status/{updateId}")
    @ApiOperation(value = "查询更新状态", notes = "根据更新ID获取知识库更新进度")
    public ResponseEntity<Map<String, Object>> queryUpdateStatus(
            @PathVariable String updateId) {
        log.info("查询知识库更新状态，更新ID: {}", updateId);
        
        try {
            Map<String, Object> status = ragKnowledgeService.queryUpdateStatus(updateId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "updateId", updateId,
                "status", status
            ));
        } catch (Exception e) {
            log.error("查询更新状态失败，更新ID: {}", updateId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "查询更新状态失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 监控边缘节点和公有云节点运行状态
     * 获取混合云架构中各节点的健康状态和性能指标
     *
     * @return ResponseEntity 包含所有节点的状态信息
     */
    @GetMapping("/node-status")
    @ApiOperation(value = "监控节点状态", notes = "获取边缘节点和公有云节点的运行状态")
    public ResponseEntity<Map<String, Object>> monitorNodeStatus() {
        log.info("开始监控RAG架构节点状态");
        
        try {
            Map<String, Object> nodeStatus = ragKnowledgeService.monitorNodeStatus();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "timestamp", System.currentTimeMillis(),
                "nodeStatus", nodeStatus
            ));
        } catch (Exception e) {
            log.error("节点状态监控失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "节点状态监控失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 备份知识库数据
     * 创建知识库的完整备份，支持指定备份路径
     *
     * @param backupPath 可选备份路径，如未指定则使用默认路径
     * @return ResponseEntity 包含备份结果信息
     */
    @PostMapping("/backup")
    @ApiOperation(value = "备份知识库", notes = "创建知识库数据的完整备份")
    public ResponseEntity<Map<String, Object>> backupKnowledgeData(
            @RequestParam(value = "backupPath", required = false) String backupPath) {
        log.info("开始备份知识库数据，备份路径: {}", backupPath != null ? backupPath : "默认路径");
        
        try {
            String backupId = ragKnowledgeService.backupKnowledgeData(backupPath);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "backupId", backupId,
                "message", "知识库备份完成",
                "backupPath", backupPath != null ? backupPath : "默认路径"
            ));
        } catch (Exception e) {
            log.error("知识库备份失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "知识库备份失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 恢复知识库数据
     * 从指定备份恢复知识库数据
     *
     * @param backupId 备份记录唯一标识
     * @return ResponseEntity 包含恢复操作结果
     */
    @PostMapping("/restore/{backupId}")
    @ApiOperation(value = "恢复知识库", notes = "从指定备份恢复知识库数据")
    public ResponseEntity<Map<String, Object>> restoreKnowledgeData(
            @PathVariable String backupId) {
        log.info("开始恢复知识库数据，备份ID: {}", backupId);
        
        try {
            boolean restoreSuccess = ragKnowledgeService.restoreKnowledgeData(backupId);
            if (restoreSuccess) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "backupId", backupId,
                    "message", "知识库恢复完成"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "知识库恢复失败"
                ));
            }
        } catch (Exception e) {
            log.error("知识库恢复失败，备份ID: {}", backupId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "知识库恢复失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取知识库基本信息
     * 返回知识库的版本、大小、最后更新时间等元数据
     *
     * @return ResponseEntity 包含知识库基本信息
     */
    @GetMapping("/info")
    @ApiOperation(value = "获取知识库信息", notes = "返回知识库的基本信息和统计")
    public ResponseEntity<Map<String, Object>> getKnowledgeBaseInfo() {
        log.info("获取知识库基本信息");
        
        try {
            KnowledgeBase knowledgeBase = ragKnowledgeService.getKnowledgeBaseInfo();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "knowledgeBase", knowledgeBase,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("获取知识库信息失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "获取知识库信息失败: " + e.getMessage()
            ));
        }
    }
}


// 内容由AI生成，仅供参考