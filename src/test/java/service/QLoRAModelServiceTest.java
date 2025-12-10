package service;

import application.service.QLoRAModelService;
import domain.entity.ModelVersion;
import domain.repository.ModelVersionRepository;
import infrastructure.config.VLLMConfig;
import infrastructure.service.ModelFineTuningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * QLoRA模型服务测试类
 * 测试模型微调和管理的各项功能
 */
@SpringBootTest
class QLoRAModelServiceTest {

    @Mock
    private ModelVersionRepository modelVersionRepository;

    @Mock
    private VLLMConfig vllmConfig;

    @Mock
    private ModelFineTuningService modelFineTuningService;

    @InjectMocks
    private QLoRAModelService qLoRAModelService;

    private ModelVersion testModelVersion;
    private ModelVersion deployedModelVersion;
    private ModelVersion readyModelVersion;
    private List<ModelVersion> modelVersionList;

    /**
     * 测试前置设置
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 初始化测试数据
        testModelVersion = new ModelVersion();
        testModelVersion.setVersionNumber("v1.0.0");
        testModelVersion.setTrainingDataSize(5000);
        testModelVersion.setAccuracyRate(0.935);
        testModelVersion.setDeploymentStatus("READY");
        testModelVersion.setCreationTime(new Date());

        deployedModelVersion = new ModelVersion();
        deployedModelVersion.setVersionNumber("v1.0.1");
        deployedModelVersion.setTrainingDataSize(5000);
        deployedModelVersion.setAccuracyRate(0.940);
        deployedModelVersion.setDeploymentStatus("DEPLOYED");
        deployedModelVersion.setCreationTime(new Date());

        readyModelVersion = new ModelVersion();
        readyModelVersion.setVersionNumber("v1.0.2");
        readyModelVersion.setTrainingDataSize(5000);
        readyModelVersion.setAccuracyRate(0.945);
        readyModelVersion.setDeploymentStatus("READY");
        readyModelVersion.setCreationTime(new Date());

        ModelVersion version1 = new ModelVersion();
        version1.setVersionNumber("v1.0.1");
        version1.setAccuracyRate(0.940);
        
        ModelVersion version2 = new ModelVersion();
        version2.setVersionNumber("v1.0.0");
        version2.setAccuracyRate(0.935);
        
        modelVersionList = Arrays.asList(version1, version2);
    }

    /**
     * 测试训练流程验证
     * 验证模型微调训练的正常流程
     */
    @Test
    @DisplayName("测试训练流程验证 - 正常流程")
    void testTrainingProcessValidation() {
        // Given - 准备测试数据
        String trainingData = "模拟训练数据集";
        String expectedTaskId = "TASK_12345";
        
        // 模拟微调服务训练执行
        when(modelFineTuningService.executeFineTuning(trainingData))
                .thenReturn(expectedTaskId);
        
        // 模拟仓储层保存操作
        when(modelVersionRepository.saveVersionInfo(any(ModelVersion.class)))
                .thenReturn(true);
        
        // When - 执行训练流程
        String taskId = qLoRAModelService.startModelFineTuning(trainingData);
        
        // Then - 验证结果
        assertNotNull(taskId, "训练任务ID不应为空");
        assertEquals(expectedTaskId, taskId, "训练任务ID应符合预期");
        
        // 验证服务调用
        verify(modelFineTuningService, times(1)).executeFineTuning(trainingData);
        
        // 验证仓储层调用
        verify(modelVersionRepository, times(1))
                .saveVersionInfo(any(ModelVersion.class));
    }

    /**
     * 测试准确率评估
     * 验证模型准确率达到93.5%以上的要求
     */
    @Test
    @DisplayName("测试准确率评估")
    void testAccuracyEvaluation() {
        // Given - 准备测试数据
        when(modelVersionRepository.sortVersionsByAccuracy())
                .thenReturn(modelVersionList);
        
        // When - 执行准确率评估
        List<ModelVersion> sortedVersions = qLoRAModelService.evaluateModelAccuracy();
        
        // Then - 验证结果
        assertNotNull(sortedVersions, "排序后的版本列表不应为空");
        assertEquals(2, sortedVersions.size(), "版本数量应符合预期");
        
        // 验证按准确率降序排列
        assertEquals(0.940, sortedVersions.get(0).getAccuracyRate(), 
                "第一个版本应为准确率最高的版本");
        assertEquals(0.935, sortedVersions.get(1).getAccuracyRate(), 
                "第二个版本应为准确率次高的版本");
        
        // 验证所有版本准确率都达到93.5%以上
        for (ModelVersion version : sortedVersions) {
            assertTrue(version.getAccuracyRate() >= 0.935, 
                    "模型准确率应达到93.5%以上");
        }
        
        // 验证仓储层调用
        verify(modelVersionRepository, times(1)).sortVersionsByAccuracy();
    }

    /**
     * 测试版本部署功能
     * 验证模型版本的正常部署流程
     */
    @Test
    @DisplayName("测试版本部署功能")
    void testVersionDeployment() {
        // Given - 准备测试数据
        String versionId = "v1.0.0";
        
        when(modelVersionRepository.queryDeploymentStatus(versionId))
                .thenReturn(Optional.of(testModelVersion));
        when(modelFineTuningService.deployModel(versionId))
                .thenReturn(true);
        when(modelVersionRepository.updateDeploymentStatus(versionId, "DEPLOYED"))
                .thenReturn(true);
        
        // When - 执行部署
        boolean deployResult = qLoRAModelService.deployNewModelVersion(versionId);
        
        // Then - 验证部署结果
        assertTrue(deployResult, "模型部署应成功");
        verify(modelVersionRepository, times(1)).queryDeploymentStatus(versionId);
        verify(modelFineTuningService, times(1)).deployModel(versionId);
        verify(modelVersionRepository, times(1)).updateDeploymentStatus(versionId, "DEPLOYED");
    }

    /**
     * 测试版本回滚功能
     * 验证从当前版本回滚到历史版本的功能
     */
    @Test
    @DisplayName("测试版本回滚功能")
    void testVersionRollback() {
        // Given - 准备测试数据
        String currentVersionId = "v1.0.1";
        String rollbackVersionId = "v1.0.0";
        
        // 模拟当前已部署版本
        when(modelVersionRepository.getCurrentlyDeployedVersion())
                .thenReturn(Optional.of(deployedModelVersion));
        
        // 模拟回滚目标版本
        when(modelVersionRepository.queryDeploymentStatus(rollbackVersionId))
                .thenReturn(Optional.of(testModelVersion));
        
        // 模拟部署服务调用
        when(modelFineTuningService.deployModel(rollbackVersionId))
                .thenReturn(true);
        
        // 模拟状态更新操作
        when(modelVersionRepository.updateDeploymentStatus(currentVersionId, "ARCHIVED"))
                .thenReturn(true);
        when(modelVersionRepository.updateDeploymentStatus(rollbackVersionId, "DEPLOYED"))
                .thenReturn(true);
        
        // When - 执行回滚
        boolean rollbackResult = qLoRAModelService.rollbackModelVersion(rollbackVersionId);
        
        // Then - 验证回滚结果
        assertTrue(rollbackResult, "模型回滚应成功");
        verify(modelVersionRepository, times(1)).getCurrentlyDeployedVersion();
        verify(modelVersionRepository, times(1)).queryDeploymentStatus(rollbackVersionId);
        verify(modelFineTuningService, times(1)).deployModel(rollbackVersionId);
        verify(modelVersionRepository, times(1)).updateDeploymentStatus(currentVersionId, "ARCHIVED");
        verify(modelVersionRepository, times(1)).updateDeploymentStatus(rollbackVersionId, "DEPLOYED");
    }

    /**
     * 测试训练进度查询
     * 验证训练进度查询功能的正确性
     */
    @Test
    @DisplayName("测试训练进度查询")
    void testTrainingProgressQuery() {
        // Given - 准备测试数据
        String taskId = "TASK_12345";
        int expectedProgress = 75;
        
        // 模拟训练进度查询
        when(modelFineTuningService.getTrainingStatus(taskId))
                .thenReturn(expectedProgress);
        
        // When - 执行进度查询
        int actualProgress = qLoRAModelService.queryTrainingProgress(taskId);
        
        // Then - 验证进度值在合理范围内
        assertTrue(actualProgress >= 0 && actualProgress <= 100, 
                "训练进度应在0-100之间");
        assertEquals(expectedProgress, actualProgress, "训练进度应符合预期");
        verify(modelFineTuningService, times(1)).getTrainingStatus(taskId);
    }

    /**
     * 测试空训练数据异常处理
     * 验证服务对空训练数据的健壮性
     */
    @Test
    @DisplayName("测试空训练数据异常处理")
    void testEmptyTrainingDataException() {
        // Given - 准备空训练数据
        String emptyTrainingData = "";
        
        // When & Then - 验证异常抛出
        assertThrows(IllegalArgumentException.class, () -> {
            qLoRAModelService.startModelFineTuning(emptyTrainingData);
        }, "空训练数据应抛出IllegalArgumentException");
        
        // 验证相关方法未被调用
        verify(modelFineTuningService, never()).executeFineTuning(anyString());
        verify(modelVersionRepository, never()).saveVersionInfo(any(ModelVersion.class));
    }

    /**
     * 测试仓储层操作失败异常处理
     * 验证服务在仓储层操作失败时的健壮性
     */
    @Test
    @DisplayName("测试仓储层操作失败异常处理")
    void testRepositoryOperationFailure() {
        // Given - 准备正常训练数据但模拟仓储层失败
        String trainingData = "正常训练数据";
        String expectedTaskId = "TASK_12345";
        
        when(modelFineTuningService.executeFineTuning(trainingData))
                .thenReturn(expectedTaskId);
        when(modelVersionRepository.saveVersionInfo(any(ModelVersion.class)))
                .thenReturn(false);
        
        // When & Then - 验证异常抛出
        assertThrows(RuntimeException.class, () -> {
            qLoRAModelService.startModelFineTuning(trainingData);
        }, "仓储层操作失败应抛出RuntimeException");
        
        // 验证微调服务被调用，但仓储保存失败
        verify(modelFineTuningService, times(1)).executeFineTuning(trainingData);
        verify(modelVersionRepository, times(1)).saveVersionInfo(any(ModelVersion.class));
    }

    /**
     * 测试部署不存在版本异常处理
     * 验证服务对不存在版本部署的健壮性
     */
    @Test
    @DisplayName("测试部署不存在版本异常处理")
    void testDeployNonExistentVersion() {
        // Given - 准备不存在的版本ID
        String nonExistentVersion = "v999.999.999";
        
        when(modelVersionRepository.queryDeploymentStatus(nonExistentVersion))
                .thenReturn(Optional.empty());
        
        // When & Then - 验证异常抛出
        assertThrows(RuntimeException.class, () -> {
            qLoRAModelService.deployNewModelVersion(nonExistentVersion);
        }, "部署不存在的版本应抛出RuntimeException");
        
        verify(modelVersionRepository, times(1)).queryDeploymentStatus(nonExistentVersion);
        verify(modelFineTuningService, never()).deployModel(anyString());
        verify(modelVersionRepository, never()).updateDeploymentStatus(anyString(), anyString());
    }

    /**
     * 测试部署非就绪状态版本异常处理
     * 验证服务对非READY状态版本部署的健壮性
     */
    @Test
    @DisplayName("测试部署非就绪状态版本异常处理")
    void testDeployNonReadyVersion() {
        // Given - 准备非READY状态的版本
        String versionId = "v1.0.1";
        ModelVersion nonReadyVersion = new ModelVersion();
        nonReadyVersion.setVersionNumber(versionId);
        nonReadyVersion.setDeploymentStatus("TRAINING");
        
        when(modelVersionRepository.queryDeploymentStatus(versionId))
                .thenReturn(Optional.of(nonReadyVersion));
        
        // When & Then - 验证异常抛出
        assertThrows(IllegalStateException.class, () -> {
            qLoRAModelService.deployNewModelVersion(versionId);
        }, "部署非READY状态版本应抛出IllegalStateException");
        
        verify(modelVersionRepository, times(1)).queryDeploymentStatus(versionId);
        verify(modelFineTuningService, never()).deployModel(anyString());
        verify(modelVersionRepository, never()).updateDeploymentStatus(anyString(), anyString());
    }

    /**
     * 测试回滚到不存在版本异常处理
     * 验证服务对回滚目标版本不存在的健壮性
     */
    @Test
    @DisplayName("测试回滚到不存在版本异常处理")
    void testRollbackToNonExistentVersion() {
        // Given - 准备不存在的回滚目标版本
        String nonExistentVersion = "v999.999.999";
        
        when(modelVersionRepository.getCurrentlyDeployedVersion())
                .thenReturn(Optional.of(deployedModelVersion));
        when(modelVersionRepository.queryDeploymentStatus(nonExistentVersion))
                .thenReturn(Optional.empty());
        
        // When & Then - 验证异常抛出
        assertThrows(RuntimeException.class, () -> {
            qLoRAModelService.rollbackModelVersion(nonExistentVersion);
        }, "回滚到不存在的版本应抛出RuntimeException");
        
        verify(modelVersionRepository, times(1)).getCurrentlyDeployedVersion();
        verify(modelVersionRepository, times(1)).queryDeploymentStatus(nonExistentVersion);
        verify(modelFineTuningService, never()).deployModel(anyString());
        verify(modelVersionRepository, never()).updateDeploymentStatus(anyString(), anyString());
    }

    /**
     * 测试无当前部署版本时回滚异常处理
     * 验证服务在没有当前部署版本时回滚的健壮性
     */
    @Test
    @DisplayName("测试无当前部署版本时回滚异常处理")
    void testRollbackWithoutCurrentDeployment() {
        // Given - 模拟没有当前部署版本
        String rollbackVersionId = "v1.0.0";
        
        when(modelVersionRepository.getCurrentlyDeployedVersion())
                .thenReturn(Optional.empty());
        
        // When & Then - 验证异常抛出
        assertThrows(IllegalStateException.class, () -> {
            qLoRAModelService.rollbackModelVersion(rollbackVersionId);
        }, "没有当前部署版本时回滚应抛出IllegalStateException");
        
        verify(modelVersionRepository, times(1)).getCurrentlyDeployedVersion();
        verify(modelVersionRepository, never()).queryDeploymentStatus(anyString());
        verify(modelFineTuningService, never()).deployModel(anyString());
        verify(modelVersionRepository, never()).updateDeploymentStatus(anyString(), anyString());
    }

    /**
     * 测试模型性能监控
     * 验证模型在线性能监控功能
     */
    @Test
    @DisplayName("测试模型性能监控")
    void testModelPerformanceMonitoring() {
        // Given - 准备性能监控数据
        QLoRAModelService.PerformanceMetrics expectedMetrics = 
                new QLoRAModelService.PerformanceMetrics(150.5, 100.0, 99.8);
        
        when(modelFineTuningService.getPerformanceMetrics())
                .thenReturn(expectedMetrics);
        
        // When - 执行性能监控
        QLoRAModelService.PerformanceMetrics performanceMetrics = 
                qLoRAModelService.monitorOnlinePerformance();
        
        // Then - 验证性能指标
        assertNotNull(performanceMetrics, "性能监控指标不应为空");
        assertEquals(150.5, performanceMetrics.getLatencyMs(), "延迟应符合预期");
        assertEquals(100.0, performanceMetrics.getThroughputQps(), "吞吐量应符合预期");
        assertEquals(99.8, performanceMetrics.getAvailabilityPercent(), "可用性应符合预期");
        
        verify(modelFineTuningService, times(1)).getPerformanceMetrics();
    }

    /**
     * 测试模型版本管理完整性
     * 验证模型版本生命周期的完整管理流程
     */
    @Test
    @DisplayName("测试模型版本管理完整性")
    void testModelVersionLifecycle() {
        // Given - 准备完整的版本管理测试数据
        String trainingData = "完整生命周期测试数据";
        String versionId = "v1.0.2";
        String taskId = "TASK_COMPLETE";
        
        // 模拟训练流程
        when(modelFineTuningService.executeFineTuning(trainingData))
                .thenReturn(taskId);
        when(modelVersionRepository.saveVersionInfo(any(ModelVersion.class)))
                .thenReturn(true);
        
        // When - 执行训练
        String actualTaskId = qLoRAModelService.startModelFineTuning(trainingData);
        assertNotNull(actualTaskId, "训练任务应成功创建");
        assertEquals(taskId, actualTaskId, "训练任务ID应符合预期");
        
        // 模拟训练完成和版本就绪
        when(modelFineTuningService.getTrainingStatus(taskId))
                .thenReturn(100); // 训练完成
        
        // 模拟部署流程
        when(modelVersionRepository.queryDeploymentStatus(versionId))
                .thenReturn(Optional.of(readyModelVersion));
        when(modelFineTuningService.deployModel(versionId))
                .thenReturn(true);
        when(modelVersionRepository.updateDeploymentStatus(versionId, "DEPLOYED"))
                .thenReturn(true);
        
        boolean deployResult = qLoRAModelService.deployNewModelVersion(versionId);
        assertTrue(deployResult, "新版本部署应成功");
        
        // 模拟回滚流程
        when(modelVersionRepository.getCurrentlyDeployedVersion())
                .thenReturn(Optional.of(readyModelVersion)); // 当前部署的是v1.0.2
        when(modelVersionRepository.queryDeploymentStatus("v1.0.1"))
                .thenReturn(Optional.of(deployedModelVersion)); // 回滚到v1.0.1
        
        when(modelFineTuningService.deployModel("v1.0.1"))
                .thenReturn(true);
        when(modelVersionRepository.updateDeploymentStatus("v1.0.2", "ARCHIVED"))
                .thenReturn(true);
        when(modelVersionRepository.updateDeploymentStatus("v1.0.1", "DEPLOYED"))
                .thenReturn(true);
        
        boolean rollbackResult = qLoRAModelService.rollbackModelVersion("v1.0.1");
        assertTrue(rollbackResult, "版本回滚应成功");
        
        // Then - 验证完整的调用链
        verify(modelFineTuningService, times(1)).executeFineTuning(trainingData);
        verify(modelVersionRepository, atLeast(2)).saveVersionInfo(any(ModelVersion.class));
        verify(modelFineTuningService, times(2)).deployModel(anyString());
        verify(modelVersionRepository, atLeast(3)).updateDeploymentStatus(anyString(), anyString());
    }

    /**
     * 测试性能监控异常处理
     * 验证性能监控服务异常时的健壮性
     */
    @Test
    @DisplayName("测试性能监控异常处理")
    void testPerformanceMonitoringException() {
        // Given - 模拟性能监控服务异常
        when(modelFineTuningService.getPerformanceMetrics())
                .thenThrow(new RuntimeException("性能监控服务不可用"));
        
        // When & Then - 验证异常抛出
        assertThrows(RuntimeException.class, () -> {
            qLoRAModelService.monitorOnlinePerformance();
        }, "性能监控服务异常时应抛出RuntimeException");
        
        verify(modelFineTuningService, times(1)).getPerformanceMetrics();
    }
}


// 内容由AI生成，仅供参考