package com.navigation.system.domain.repository;

import com.navigation.system.domain.entity.KnowledgeBase;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 知识库领域仓储接口
 * 提供知识库的分布式管理操作，包括保存知识数据、同步边缘节点和公有云数据、备份和恢复操作等方法
 * 
 * @author Navigation System Team
 * @version 1.0
 */
public interface KnowledgeBaseRepository {
    
    /**
     * 根据ID查询知识库实体
     * 
     * @param id 知识库ID
     * @return 知识库实体，如果不存在则返回空
     */
    Optional<KnowledgeBase> findById(Long id);
    
    /**
     * 保存知识库实体
     * 
     * @param knowledgeBase 知识库实体
     * @return 保存后的知识库实体
     */
    KnowledgeBase save(KnowledgeBase knowledgeBase);
    
    /**
     * 根据场所ID查询最新的知识库数据
     * 
     * @param venueId 场所ID
     * @return 最新的知识库实体，如果不存在则返回空
     */
    Optional<KnowledgeBase> findLatestByVenueId(Long venueId);
    
    /**
     * 查询需要同步到边缘节点的知识库记录
     * 
     * @param lastSyncTime 上次同步时间
     * @return 需要同步的知识库列表
     */
    List<KnowledgeBase> findPendingEdgeSyncRecords(Date lastSyncTime);
    
    /**
     * 查询需要同步到公有云节点的知识库记录
     * 
     * @param lastSyncTime 上次同步时间
     * @return 需要同步的知识库列表
     */
    List<KnowledgeBase> findPendingCloudSyncRecords(Date lastSyncTime);
    
    /**
     * 根据向量索引状态查询知识库记录
     * 
     * @param indexStatus 向量索引状态
     * @return 符合条件的知识库列表
     */
    List<KnowledgeBase> findByVectorIndexStatus(String indexStatus);
    
    /**
     * 更新知识库的同步状态
     * 
     * @param knowledgeId 知识库ID
     * @param syncStatus 新的同步状态
     * @return 更新是否成功
     */
    boolean updateSyncStatus(Long knowledgeId, String syncStatus);
    
    /**
     * 更新知识库的云端同步状态
     * 
     * @param knowledgeId 知识库ID
     * @param cloudSyncStatus 新的云端同步状态
     * @return 更新是否成功
     */
    boolean updateCloudSyncStatus(Long knowledgeId, String cloudSyncStatus);
    
    /**
     * 更新知识库的备份状态
     * 
     * @param knowledgeId 知识库ID
     * @param backupStatus 备份状态
     * @param backupPath 备份文件路径
     * @return 更新是否成功
     */
    boolean updateBackupStatus(Long knowledgeId, String backupStatus, String backupPath);
    
    /**
     * 查询需要备份的知识库记录
     * 
     * @return 需要备份的知识库列表
     */
    List<KnowledgeBase> findRecordsNeedBackup();
    
    /**
     * 根据备份状态查询知识库记录
     * 
     * @param backupStatus 备份状态
     * @return 符合条件的知识库列表
     */
    List<KnowledgeBase> findByBackupStatus(String backupStatus);
    
    /**
     * 协调数据同步到指定节点类型
     * 
     * @param knowledgeId 知识库ID
     * @param nodeTypes 节点类型列表（EDGE, CLOUD）
     * @return 同步是否成功
     */
    boolean syncDataToNodes(Long knowledgeId, List<String> nodeTypes);
    
    /**
     * 查找指定时间戳后的增量更新
     * 
     * @param timestamp 时间戳
     * @return 增量更新的知识库列表
     */
    List<KnowledgeBase> findIncrementalUpdatesAfter(Date timestamp);
    
    /**
     * 根据节点状态查询知识库记录
     * 
     * @param nodeStatus 节点状态
     * @return 符合条件的知识库列表
     */
    List<KnowledgeBase> findByNodeStatus(String nodeStatus);
    
    /**
     * 从备份恢复知识库数据
     * 
     * @param backupPath 备份文件路径
     * @return 恢复的知识库实体
     */
    Optional<KnowledgeBase> restoreFromBackup(String backupPath);
    
    /**
     * 删除知识库记录
     * 
     * @param id 知识库ID
     */
    void deleteById(Long id);
    
    /**
     * 检查知识库是否存在
     * 
     * @param id 知识库ID
     * @return 是否存在
     */
    boolean existsById(Long id);
}


// 内容由AI生成，仅供参考