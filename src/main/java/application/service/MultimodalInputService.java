package com.navigation.application.service;

import com.navigation.domain.entity.MultimodalInput;
import com.navigation.domain.enums.InputType;
import com.navigation.domain.repository.MultimodalInputRepository;
import com.navigation.domain.valueobject.FeatureVector;
import com.navigation.domain.valueobject.LocationInfo;
import com.navigation.infrastructure.client.CLIPClient;
import com.navigation.infrastructure.client.QianWenVLClient;
import com.navigation.infrastructure.client.VLLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 多模态输入处理服务实现类
 * 提供多模态数据的解析和特征提取服务
 */
@Service
public class MultimodalInputServiceImpl implements MultimodalInputService {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalInputServiceImpl.class);
    
    private final MultimodalInputRepository multimodalInputRepository;
    private final QianWenVLClient qianWenVLClient;
    private final CLIPClient clipClient;
    private final VLLMClient vllmClient;
    
    /**
     * 构造函数注入依赖
     */
    @Autowired
    public MultimodalInputServiceImpl(MultimodalInputRepository multimodalInputRepository,
                                     QianWenVLClient qianWenVLClient,
                                     CLIPClient clipClient,
                                     VLLMClient vllmClient) {
        this.multimodalInputRepository = multimodalInputRepository;
        this.qianWenVLClient = qianWenVLClient;
        this.clipClient = clipClient;
        this.vllmClient = vllmClient;
    }

    /**
     * 调用千问VL模型解析图片内容
     */
    @Override
    public LocationInfo parseImageContent(MultipartFile imageFile) {
        logger.info("开始解析图片内容，文件名: {}", imageFile.getOriginalFilename());
        
        try {
            LocationInfo locationInfo = qianWenVLClient.analyzeImage(imageFile);
            
            saveInputRecord(InputType.IMAGE, locationInfo.toString(), getCurrentUserId());
            logger.info("图片内容解析成功，位置信息: {}", locationInfo);
            
            return locationInfo;
        } catch (Exception e) {
            logger.error("图片内容解析失败，文件名: {}", imageFile.getOriginalFilename(), e);
            throw new RuntimeException("图片内容解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用CLIP模型提取文本特征
     */
    @Override
    public FeatureVector extractTextFeatures(String text) {
        logger.info("开始提取文本特征，文本长度: {}", text.length());
        
        try {
            FeatureVector featureVector = clipClient.extractTextFeatures(text);
            
            saveInputRecord(InputType.TEXT, featureVector.toString(), getCurrentUserId());
            logger.debug("文本特征提取成功，特征维度: {}", featureVector.getDimension());
            
            return featureVector;
        } catch (Exception e) {
            logger.error("文本特征提取失败，文本: {}", text.substring(0, Math.min(text.length(), 100)), e);
            throw new RuntimeException("文本特征提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用CLIP模型提取视频特征
     */
    @Override
    public List<FeatureVector> extractVideoFeatures(List<MultipartFile> videoFrames) {
        logger.info("开始提取视频特征，帧数: {}", videoFrames.size());
        
        try {
            List<FeatureVector> continuousFeatures = clipClient.extractVideoFeatures(videoFrames);
            
            String featuresString = continuousFeatures.stream()
                    .map(FeatureVector::toString)
                    .collect(Collectors.joining(";"));
            saveInputRecord(InputType.VIDEO, featuresString, getCurrentUserId());
            logger.info("视频特征提取成功，特征序列长度: {}", continuousFeatures.size());
            
            return continuousFeatures;
        } catch (Exception e) {
            logger.error("视频特征提取失败，帧数: {}", videoFrames.size(), e);
            throw new RuntimeException("视频特征提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过vLLM引擎实现并发处理
     */
    @Override
    public List<LocationInfo> concurrentProcessing(List<MultimodalInput> inputs) {
        logger.info("开始并发处理多模态输入，输入数量: {}", inputs.size());
        
        try {
            // 使用vLLM引擎进行批量推理
            List<LocationInfo> results = vllmClient.batchProcess(inputs);
            logger.info("并发处理完成，成功处理 {} 个输入", results.size());
            
            return results;
        } catch (Exception e) {
            logger.error("并发处理失败，输入数量: {}", inputs.size(), e);
            throw new RuntimeException("并发处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 输出用户位置和目标位置信息
     */
    @Override
    public LocationInfo outputLocationInfo(MultimodalInput multimodalInput) {
        logger.info("开始处理位置信息，输入类型: {}", multimodalInput.getInputType());
        
        try {
            LocationInfo locationInfo;
            switch (multimodalInput.getInputType()) {
                case IMAGE:
                    locationInfo = processImageLocation(multimodalInput.getDataContent());
                    break;
                case TEXT:
                    locationInfo = processTextLocation(multimodalInput.getDataContent());
                    break;
                case VIDEO:
                    locationInfo = processVideoLocation(multimodalInput.getDataContent());
                    break;
                default:
                    throw new IllegalArgumentException("不支持的输入类型: " + multimodalInput.getInputType());
            }
            logger.debug("位置信息处理完成: {}", locationInfo);
            return locationInfo;
        } catch (IllegalArgumentException e) {
            logger.warn("不支持的输入类型: {}", multimodalInput.getInputType());
            throw e;
        } catch (Exception e) {
            logger.error("位置信息输出失败，输入ID: {}", multimodalInput.getId(), e);
            throw new RuntimeException("位置信息输出失败: " + e.getMessage(), e);
        }
    }

    // 私有辅助方法

    /**
     * 异步处理单个输入
     */
    @Async
    @Override
    public CompletableFuture<LocationInfo> processInputAsync(MultimodalInput input) {
        return CompletableFuture.supplyAsync(() -> outputLocationInfo(input));
    }

    /**
     * 保存输入记录到数据库
     */
    private void saveInputRecord(InputType inputType, String dataContent, Long userId) {
        try {
            MultimodalInput inputRecord = MultimodalInput.builder()
                    .inputType(inputType)
                    .dataContent(dataContent)
                    .timestamp(new Date())
                    .userId(userId)
                    .build();
            
            multimodalInputRepository.saveInputRecord(inputRecord);
            logger.debug("输入记录保存成功，类型: {}, 用户ID: {}", inputType, userId);
        } catch (Exception e) {
            logger.error("保存输入记录失败，类型: {}, 用户ID: {}", inputType, userId, e);
            throw new RuntimeException("保存输入记录失败", e);
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("未找到认证信息，使用默认用户ID");
                return 0L;
            }
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // 从UserDetails中获取用户ID
                String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                // 这里需要根据实际的用户服务来将username转换为userId
                return convertUsernameToUserId(username);
            } else if (principal instanceof String) {
                return convertUsernameToUserId((String) principal);
            } else {
                logger.warn("不支持的Principal类型: {}", principal.getClass().getName());
                return 0L;
            }
        } catch (Exception e) {
            logger.error("获取当前用户ID失败", e);
            return 0L;
        }
    }

    /**
     * 处理图片位置信息
     */
    private LocationInfo processImageLocation(String imageContent) {
        // 集成实际的路径规划算法和知识库查询
        // 这里应该调用RAGKnowledgeService进行环境匹配
        // 调用RealTimeNavigationService进行路径规划
        logger.debug("处理图片位置信息: {}", imageContent);
        return new LocationInfo("当前位置", "目标位置", "基于图片分析的导航路径");
    }

    /**
     * 处理文本位置信息
     */
    private LocationInfo processTextLocation(String textContent) {
        // 集成实际的意图识别和位置匹配算法
        logger.debug("处理文本位置信息: {}", textContent);
        return new LocationInfo("当前位置", "目标位置", "基于文本描述的导航路径");
    }

    /**
     * 处理视频位置信息
     */
    private LocationInfo processVideoLocation(String videoContent) {
        // 集成实际的实时定位算法
        logger.debug("处理视频位置信息: {}", videoContent);
        return new LocationInfo("当前位置", "目标位置", "基于视频流的实时导航路径");
    }

    /**
     * 将用户名转换为用户ID
     */
    private Long convertUsernameToUserId(String username) {
        // TODO: 这里需要集成用户服务来实际转换
        // 目前返回一个基于用户名的哈希值作为模拟
        try {
            return (long) username.hashCode();
        } catch (Exception e) {
            logger.warn("用户名转换用户ID失败: {}", username, e);
            return 0L;
        }
    }
}


// 内容由AI生成，仅供参考