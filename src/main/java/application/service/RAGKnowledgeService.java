package com.navigation.application.service;

import com.navigation.domain.entity.KnowledgeBase;
import com.navigation.domain.repository.KnowledgeBaseRepository;
import com.navigation.infrastructure.config.FAISSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 混合云RAG架构管理应用服务类
 * 提供知识库的分布式管理服务，包括数据同步、向量索引构建、安全传输等功能
 */
@Service
public class RAGKnowledgeService {

    private static final Logger logger = LoggerFactory.getLogger(RAGKnowledgeService.class);
    
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final FAISSConfig faissConfig;
    private final RestTemplate restTemplate;
    
    // JWT签名密钥（从配置中读取）
    private final SecretKey signingKey;

    /**
     * 节点状态信息类
     */
    public static class NodeStatus {
        private final String url;
        private final String status;
        private final Long responseTime;
        private final String details;
        
        public NodeStatus(String url, String status, Long responseTime, String details) {
            this.url = url;
            this.status = status;
            this.responseTime = responseTime;
            this.details = details;
        }
        
        // Getters
        public String getUrl() { return url; }
        public String getStatus() { return status; }
        public Long getResponseTime() { return responseTime; }
        public String getDetails() { return details; }
        
        @Override
        public String toString() {
            return String.format("NodeStatus{url='%s', status='%s', responseTime=%d, details='%s'}",
                               url, status, responseTime, details);
        }
    }

    /**
     * 自定义异常类
     */
    public static class KnowledgeSyncException extends RuntimeException {
        public KnowledgeSyncException(String message) { super(message); }
        public KnowledgeSyncException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class KnowledgeUpdateException extends RuntimeException {
        public KnowledgeUpdateException(String message) { super(message); }
        public KnowledgeUpdateException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * 构造函数注入依赖
     *
     * @param knowledgeBaseRepository 知识库仓储接口
     * @param faissConfig FAISS配置类
     * @param restTemplate HTTP客户端
     * @param jwtSecret JWT密钥（从配置注入）
     */
    @Autowired
    public RAGKnowledgeService(KnowledgeBaseRepository knowledgeBaseRepository, 
                              FAISSConfig faissConfig, 
                              RestTemplate restTemplate,
                              @Value("${app.security.jwt-secret}") String jwtSecret) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.faissConfig = faissConfig;
        this.restTemplate = restTemplate;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 协调公有云和边缘节点的数据同步
     *
     * @param cloudNodeUrl 公有云节点URL
     * @param edgeNodeUrl  边缘节点URL
     * @param knowledgeData 知识数据
     * @param ruleText 规则文本
     * @return boolean 同步是否成功
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean coordinateDataSync(String cloudNodeUrl, String edgeNodeUrl, 
                                     String knowledgeData, String ruleText) {
        try {
            // 保存知识数据到本地数据库
            KnowledgeBase knowledgeBase = new KnowledgeBase();
            knowledgeBase.setVenueMapData(knowledgeData);
            knowledgeBase.setRuleText(ruleText);
            knowledgeBase.setUpdateTime(new Date());
            
            // 构建向量索引
            String vectorIndex = buildVectorIndexInternal(knowledgeData, ruleText);
            knowledgeBase.setVectorIndex(vectorIndex);
            
            knowledgeBaseRepository.saveKnowledgeData(knowledgeBase);

            // 异步执行节点同步
            performNodeSync(cloudNodeUrl, edgeNodeUrl, knowledgeData, ruleText, vectorIndex);
            
            return true;
        } catch (Exception e) {
            throw new KnowledgeSyncException("数据同步协调失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用FAISS构建和更新向量索引
     *
     * @param knowledgeData 知识数据
     * @param ruleText 规则文本
     * @return String 向量索引ID
     */
    public String buildVectorIndex(String knowledgeData, String ruleText) {
        try {
            String vectorIndex = buildVectorIndexInternal(knowledgeData, ruleText);
            
            // 获取或创建知识库记录
            KnowledgeBase latestKnowledge = knowledgeBaseRepository.findLatestKnowledge();
            if (latestKnowledge == null) {
                latestKnowledge = new KnowledgeBase();
                latestKnowledge.setVenueMapData(knowledgeData);
                latestKnowledge.setRuleText(ruleText);
            }
            
            latestKnowledge.setVectorIndex(vectorIndex);
            latestKnowledge.setUpdateTime(new Date());
            knowledgeBaseRepository.saveKnowledgeData(latestKnowledge);
            
            return vectorIndex;
        } catch (Exception e) {
            throw new KnowledgeSyncException("向量索引构建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过HTTPS+JWT安全传输数据
     *
     * @param targetUrl 目标URL
     * @param data 传输数据
     * @param jwtToken JWT令牌
     * @return boolean 传输是否成功
     */
    public boolean secureDataTransmission(String targetUrl, String data, String jwtToken) {
        try {
            // 验证JWT令牌有效性
            if (!validateJwtToken(jwtToken)) {
                throw new SecurityException("JWT令牌验证失败");
            }

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> requestEntity = new HttpEntity<>(data, headers);
            
            // 发送HTTPS请求
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl, HttpMethod.POST, requestEntity, String.class);
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            throw new KnowledgeSyncException("安全数据传输失败: " + e.getMessage(), e);
        }
    }

    /**
     * 实现知识库增量更新
     *
     * @param incrementalData 增量地图数据
     * @param incrementalRules 增量规则文本
     * @return boolean 更新是否成功
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean incrementalKnowledgeUpdate(String incrementalData, String incrementalRules) {
        try {
            // 获取当前知识库最新版本
            KnowledgeBase currentKnowledge = knowledgeBaseRepository.findLatestKnowledge();
            if (currentKnowledge == null) {
                currentKnowledge = new KnowledgeBase();
            }

            // 合并增量数据
            String updatedData = mergeIncrementalData(
                currentKnowledge.getVenueMapData(), incrementalData);
            String updatedRules = mergeIncrementalData(
                currentKnowledge.getRuleText(), incrementalRules);
                
            currentKnowledge.setVenueMapData(updatedData);
            currentKnowledge.setRuleText(updatedRules);
            currentKnowledge.setUpdateTime(new Date());

            // 重新构建向量索引
            String newVectorIndex = buildVectorIndexInternal(updatedData, updatedRules);
            currentKnowledge.setVectorIndex(newVectorIndex);

            // 保存更新后的知识库
            knowledgeBaseRepository.saveKnowledgeData(currentKnowledge);

            return true;
        } catch (Exception e) {
            throw new KnowledgeUpdateException("知识库增量更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 监控架构节点状态
     *
     * @param nodeUrls 节点URL列表
     * @return List<NodeStatus> 各节点状态信息
     */
    public List<NodeStatus> monitorNodeStatus(List<String> nodeUrls) {
        try {
            return nodeUrls.stream()
                    .map(this::checkNodeHealth)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("节点状态监控失败: " + e.getMessage(), e);
        }
    }

    /**
     * 备份知识库数据
     *
     * @param backupPath 备份路径
     * @return boolean 备份是否成功
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean backupKnowledgeData(String backupPath) {
        try {
            KnowledgeBase knowledgeBase = knowledgeBaseRepository.findLatestKnowledge();
            if (knowledgeBase == null) {
                throw new IllegalStateException("无可用的知识库数据进行备份");
            }

            return knowledgeBaseRepository.backupAndRestore(knowledgeBase, backupPath, "backup");
        } catch (Exception e) {
            throw new KnowledgeSyncException("知识库备份失败: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复知识库数据
     *
     * @param backupPath 备份文件路径
     * @return boolean 恢复是否成功
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean restoreKnowledgeData(String backupPath) {
        try {
            return knowledgeBaseRepository.backupAndRestore(null, backupPath, "restore");
        } catch (Exception e) {
            throw new KnowledgeSyncException("知识库恢复失败: " + e.getMessage(), e);
        }
    }

    // ========== 私有方法 ==========

    /**
     * 内部向量索引构建方法
     */
    private String buildVectorIndexInternal(String knowledgeData, String ruleText) {
        try {
            // 组合文本数据进行向量化
            String combinedText = knowledgeData + " " + ruleText;
            
            // 使用FAISS配置构建向量索引
            return faissConfig.buildIndex(combinedText);
        } catch (Exception e) {
            throw new KnowledgeSyncException("向量索引构建内部错误: " + e.getMessage(), e);
        }
    }

    /**
     * 异步执行节点数据同步
     */
    @Async
    protected void performNodeSync(String cloudNodeUrl, String edgeNodeUrl, 
                                  String knowledgeData, String ruleText, String vectorIndex) {
        try {
            // 生成同步令牌
            String syncToken = generateSyncToken();
            
            // 向公有云节点同步数据
            boolean cloudSyncResult = syncWithCloudNode(cloudNodeUrl, knowledgeData, ruleText, vectorIndex, syncToken);
            
            // 向边缘节点同步数据
            boolean edgeSyncResult = syncWithEdgeNode(edgeNodeUrl, knowledgeData, ruleText, vectorIndex, syncToken);

            if (!cloudSyncResult || !edgeSyncResult) {
                logger.warn("节点同步部分失败 - 云节点: {}, 边缘节点: {}", cloudSyncResult, edgeSyncResult);
            } else {
                logger.info("节点同步成功完成");
            }
        } catch (Exception e) {
            logger.error("节点同步异常", e);
        }
    }

    /**
     * 与公有云节点同步数据
     */
    private boolean syncWithCloudNode(String cloudNodeUrl, String knowledgeData, 
                                     String ruleText, String vectorIndex, String syncToken) {
        try {
            String syncData = createSyncPayload(knowledgeData, ruleText, vectorIndex, syncToken);
            return secureDataTransmission(cloudNodeUrl + "/api/sync", syncData, syncToken);
        } catch (Exception e) {
            logger.error("公有云节点同步失败: {}", cloudNodeUrl, e);
            return false;
        }
    }

    /**
     * 与边缘节点同步数据
     */
    private boolean syncWithEdgeNode(String edgeNodeUrl, String knowledgeData, 
                                    String ruleText, String vectorIndex, String syncToken) {
        try {
            String syncData = createSyncPayload(knowledgeData, ruleText, vectorIndex, syncToken);
            return secureDataTransmission(edgeNodeUrl + "/api/sync", syncData, syncToken);
        } catch (Exception e) {
            logger.error("边缘节点同步失败: {}", edgeNodeUrl, e);
            return false;
        }
    }

    /**
     * 创建同步数据负载
     */
    private String createSyncPayload(String knowledgeData, String ruleText, 
                                   String vectorIndex, String syncToken) {
        // 简化实现，实际应使用JSON对象
        return String.format("{\"knowledgeData\":\"%s\",\"ruleText\":\"%s\",\"vectorIndex\":\"%s\",\"syncToken\":\"%s\"}",
                           knowledgeData, ruleText, vectorIndex, syncToken);
    }

    /**
     * 生成同步令牌
     */
    private String generateSyncToken() {
        return Jwts.builder()
                .setSubject("sync-token")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 300000)) // 5分钟过期
                .signWith(signingKey)
                .compact();
    }

    /**
     * 验证JWT令牌
     */
    private boolean validateJwtToken(String jwtToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody();
            
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            logger.warn("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 智能合并增量数据
     */
    private String mergeIncrementalData(String currentData, String incrementalData) {
        if (currentData == null || currentData.isEmpty()) {
            return incrementalData != null ? incrementalData : "";
        }
        if (incrementalData == null || incrementalData.isEmpty()) {
            return currentData;
        }
        
        // 改进的合并逻辑：尝试JSON合并，否则使用智能拼接
        try {
            return mergeJsonData(currentData, incrementalData);
        } catch (Exception e) {
            logger.debug("数据不是标准JSON格式，使用智能拼接策略");
            return mergeTextData(currentData, incrementalData);
        }
    }

    /**
     * JSON数据合并（基础实现）
     */
    private String mergeJsonData(String currentData, String incrementalData) {
        // 简化的JSON合并逻辑，实际应根据具体JSON结构实现
        // 这里假设是简单的键值对合并
        return currentData.substring(0, currentData.length() - 1) + "," +
               incrementalData.substring(1);
    }

    /**
     * 文本数据智能拼接
     */
    private String mergeTextData(String currentData, String incrementalData) {
        // 避免重复内容的简单策略
        if (currentData.contains(incrementalData)) {
            return currentData;
        }
        return currentData + " | " + incrementalData;
    }

    /**
     * 检查节点健康状态
     */
    private NodeStatus checkNodeHealth(String nodeUrl) {
        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(nodeUrl + "/health", String.class);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return new NodeStatus(nodeUrl, "HEALTHY", responseTime, response.getBody());
            } else {
                return new NodeStatus(nodeUrl, "UNHEALTHY", responseTime, 
                                    "HTTP Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new NodeStatus(nodeUrl, "OFFLINE", responseTime, e.getMessage());
        }
    }
}


// 内容由AI生成，仅供参考