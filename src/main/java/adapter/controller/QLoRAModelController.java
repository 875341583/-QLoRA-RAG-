package adapter.controller;

import application.service.QLoRAModelService;
import domain.entity.ModelVersion;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * QLoRA模型管理控制器
 * 负责处理模型微调、版本管理、性能监控等HTTP请求
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/qloramodel")
@Api(tags = "QLoRA模型管理接口")
public class QLoRAModelController {

    private final QLoRAModelService qLoRAModelService;

    /**
     * 构造函数注入QLoRA模型服务
     *
     * @param qLoRAModelService QLoRA模型服务实例
     */
    @Autowired
    public QLoRAModelController(QLoRAModelService qLoRAModelService) {
        this.qLoRAModelService = qLoRAModelService;
    }

    /**
     * 启动模型微调训练任务
     *
     * @param trainingData 训练数据路径
     * @return 训练任务ID
     */
    @PostMapping("/start-fine-tuning")
    @ApiOperation(value = "启动模型微调训练", notes = "使用5000+样本数据启动QLoRA微调训练")
    public ResponseEntity<String> startModelFineTuning(@RequestParam String trainingData) {
        try {
            log.info("接收到模型微调请求，训练数据路径: {}", trainingData);
            String taskId = qLoRAModelService.startModelFineTuning(trainingData);
            log.info("模型微调任务启动成功，任务ID: {}", taskId);
            return ResponseEntity.ok(taskId);
        } catch (Exception e) {
            log.error("启动模型微调任务失败，训练数据: {}", trainingData, e);
            return ResponseEntity.internalServerError().body("启动训练任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询模型训练进度
     *
     * @param taskId 训练任务ID
     * @return 训练进度百分比（0-100之间的数值）
     */
    @GetMapping("/training-progress")
    @ApiOperation(value = "查询训练进度", notes = "根据任务ID查询模型微调训练进度")
    public ResponseEntity<Double> queryTrainingProgress(@RequestParam String taskId) {
        try {
            log.debug("查询训练进度，任务ID: {}", taskId);
            Double progress = qLoRAModelService.queryTrainingProgress(taskId);
            // Service层返回的是0-1之间的小数，转换为百分比
            double progressPercentage = progress * 100;
            log.info("任务 {} 的训练进度: {:.2f}%", taskId, progressPercentage);
            return ResponseEntity.ok(progressPercentage);
        } catch (Exception e) {
            log.error("查询训练进度失败，任务ID: {}", taskId, e);
            // 返回0.0表示进度未知或查询失败，比-1.0更符合业务语义
            return ResponseEntity.ok(0.0);
        }
    }

    /**
     * 部署新版本模型
     *
     * @param versionId 模型版本ID
     * @return 部署结果
     */
    @PostMapping("/deploy-model")
    @ApiOperation(value = "部署新版本模型", notes = "将指定版本的模型部署到生产环境")
    public ResponseEntity<String> deployNewModelVersion(@RequestParam String versionId) {
        try {
            log.info("接收到模型部署请求，版本ID: {}", versionId);
            String result = qLoRAModelService.deployNewModelVersion(versionId);
            log.info("模型版本 {} 部署成功", versionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("部署模型版本失败，版本ID: {}", versionId, e);
            return ResponseEntity.internalServerError().body("部署失败: " + e.getMessage());
        }
    }

    /**
     * 回滚到历史模型版本
     *
     * @param versionId 目标回滚版本ID
     * @return 回滚结果
     */
    @PostMapping("/rollback-model")
    @ApiOperation(value = "回滚模型版本", notes = "将当前模型回滚到指定的历史版本")
    public ResponseEntity<String> rollbackModelVersion(@RequestParam String versionId) {
        try {
            log.info("接收到模型回滚请求，目标版本ID: {}", versionId);
            String result = qLoRAModelService.rollbackModelVersion(versionId);
            log.info("模型成功回滚到版本 {}", versionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("回滚模型版本失败，目标版本ID: {}", versionId, e);
            return ResponseEntity.internalServerError().body("回滚失败: " + e.getMessage());
        }
    }

    /**
     * 监控模型在线性能指标
     *
     * @param versionId 模型版本ID（可选，为空时监控当前活跃版本）
     * @return 性能指标数据
     */
    @GetMapping("/monitor-performance")
    @ApiOperation(value = "监控模型性能", notes = "监控指定版本或当前活跃模型的在线性能指标")
    public ResponseEntity<ModelVersion> monitorModelPerformance(
            @RequestParam(required = false) String versionId) {
        try {
            log.debug("监控模型性能，版本ID: {}", versionId != null ? versionId : "当前活跃版本");
            ModelVersion performance = qLoRAModelService.monitorModelPerformance(versionId);
            log.info("模型性能监控完成，准确率: {:.2f}%", performance.getAccuracyRate() * 100);
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            log.error("监控模型性能失败，版本ID: {}", versionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取所有模型版本列表
     *
     * @return 模型版本列表
     */
    @GetMapping("/versions")
    @ApiOperation(value = "获取模型版本列表", notes = "获取系统中所有可用的模型版本信息")
    public ResponseEntity<List<ModelVersion>> getAllModelVersions() {
        try {
            log.debug("获取所有模型版本列表");
            List<ModelVersion> versions = qLoRAModelService.getAllModelVersions();
            log.info("成功获取 {} 个模型版本", versions.size());
            return ResponseEntity.ok(versions);
        } catch (Exception e) {
            log.error("获取模型版本列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}


// 内容由AI生成，仅供参考