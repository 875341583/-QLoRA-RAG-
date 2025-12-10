package com.navigation.system.domain.repository;

import com.navigation.system.domain.entity.MultimodalInput;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 多模态输入领域仓储接口
 * 提供多模态输入数据的持久化操作，遵循DDD领域驱动设计原则
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Repository
public interface MultimodalInputRepository extends JpaRepository<MultimodalInput, Long> {
    
    /**
     * 根据用户ID查询多模态输入记录，按时间倒序排列
     * 
     * @param userId 用户唯一标识
     * @return 用户的多模态输入记录列表，按创建时间降序排列
     */
    List<MultimodalInput> findByUserIdOrderByTimestampDesc(Long userId);
    
    /**
     * 根据用户ID和分页参数查询多模态输入记录
     * 
     * @param userId 用户唯一标识
     * @param pageable 分页参数
     * @return 用户的多模态输入记录分页列表，按创建时间降序排列
     */
    List<MultimodalInput> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    /**
     * 根据处理状态查询多模态输入记录
     * 
     * @param processingStatus 处理状态（如：PENDING, PROCESSING, COMPLETED, FAILED）
     * @return 指定状态的多模态输入记录列表
     */
    List<MultimodalInput> findByProcessingStatus(String processingStatus);
    
    /**
     * 根据处理状态和分页参数查询多模态输入记录
     * 
     * @param processingStatus 处理状态
     * @param pageable 分页参数
     * @return 指定状态的多模态输入记录分页列表
     */
    List<MultimodalInput> findByProcessingStatus(String processingStatus, Pageable pageable);
    
    /**
     * 根据输入类型查询最近的多模态输入记录
     * 
     * @param inputType 输入类型（IMAGE, TEXT, VIDEO）
     * @param pageable 分页参数，用于限制返回记录数量
     * @return 指定类型的多模态输入记录列表，按时间倒序排列
     */
    @Query("SELECT mi FROM MultimodalInput mi WHERE mi.inputType = :inputType ORDER BY mi.timestamp DESC")
    List<MultimodalInput> findRecentByInputType(@Param("inputType") String inputType, Pageable pageable);
    
    /**
     * 根据记录ID和用户ID查询特定的多模态输入记录
     * 用于权限验证，确保用户只能访问自己的记录
     * 
     * @param id 记录唯一标识
     * @param userId 用户唯一标识
     * @return 匹配的多模态输入记录，如果不存在则返回空
     */
    Optional<MultimodalInput> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 统计指定用户在某个时间段内的多模态输入数量
     * 
     * @param userId 用户唯一标识
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段内的输入记录数量
     */
    @Query("SELECT COUNT(mi) FROM MultimodalInput mi WHERE mi.userId = :userId AND mi.timestamp BETWEEN :startTime AND :endTime")
    Long countByUserIdAndTimestampBetween(@Param("userId") Long userId, 
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定处理状态的记录数量
     * 
     * @param processingStatus 处理状态
     * @return 指定状态的记录数量
     */
    Long countByProcessingStatus(String processingStatus);
    
    /**
     * 批量更新多模态输入记录的处理状态
     * 用于批量处理场景，提高数据库操作效率
     * 
     * @param ids 需要更新的记录ID列表
     * @param newStatus 新的处理状态
     * @return 更新的记录数量
     */
    @Modifying
    @Query("UPDATE MultimodalInput mi SET mi.processingStatus = :newStatus, mi.lastUpdatedTime = CURRENT_TIMESTAMP WHERE mi.id IN :ids")
    int updateProcessingStatusByIds(@Param("ids") List<Long> ids, @Param("newStatus") String newStatus);
    
    /**
     * 更新单条记录的处理状态和重试次数
     * 
     * @param id 记录ID
     * @param newStatus 新的处理状态
     * @param retryCount 重试次数
     * @return 更新的记录数量
     */
    @Modifying
    @Query("UPDATE MultimodalInput mi SET mi.processingStatus = :newStatus, mi.retryCount = :retryCount, mi.lastUpdatedTime = CURRENT_TIMESTAMP WHERE mi.id = :id")
    int updateStatusAndRetryCountById(@Param("id") Long id, @Param("newStatus") String newStatus, @Param("retryCount") Integer retryCount);
    
    /**
     * 查找需要重试的失败记录（失败次数小于最大重试次数且在指定时间窗口内）
     * 
     * @param maxRetryCount 最大重试次数
     * @param retryTimeWindowStart 重试时间窗口的起始时间
     * @return 需要重试的多模态输入记录列表
     */
    @Query("SELECT mi FROM MultimodalInput mi WHERE mi.processingStatus = 'FAILED' AND mi.retryCount < :maxRetryCount AND mi.timestamp >= :retryTimeWindowStart ORDER BY mi.timestamp ASC")
    List<MultimodalInput> findFailedRecordsForRetry(@Param("maxRetryCount") Integer maxRetryCount, 
                                                   @Param("retryTimeWindowStart") LocalDateTime retryTimeWindowStart);
    
    /**
     * 根据用户ID和处理状态查询记录
     * 
     * @param userId 用户ID
     * @param processingStatus 处理状态
     * @return 匹配的记录列表
     */
    List<MultimodalInput> findByUserIdAndProcessingStatus(Long userId, String processingStatus);
    
    /**
     * 删除指定时间之前的陈旧记录（用于数据清理）
     * 
     * @param cutoffTime 截止时间
     * @return 删除的记录数量
     */
    @Modifying
    @Query("DELETE FROM MultimodalInput mi WHERE mi.timestamp < :cutoffTime")
    int deleteRecordsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 查找超时的处理中记录（用于监控和恢复）
     * 
     * @param timeoutThreshold 超时阈值
     * @return 超时的处理中记录列表
     */
    @Query("SELECT mi FROM MultimodalInput mi WHERE mi.processingStatus = 'PROCESSING' AND mi.timestamp < :timeoutThreshold")
    List<MultimodalInput> findTimedOutProcessingRecords(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
}


// 内容由AI生成，仅供参考