package com.navigation.system.infrastructure.sql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.navigation.system.domain.entity.MultimodalInput;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Date;

/**
 * 多模态输入数据映射器接口
 * 负责多模态输入数据的数据库操作，包括插入输入记录、更新处理状态、按用户查询历史等SQL映射
 * 
 * @author Navigation System Team
 * @version 1.0
 */
@Mapper
public interface MultimodalInputMapper extends BaseMapper<MultimodalInput> {
    
    /**
     * 插入多模态输入记录
     * 
     * @param multimodalInput 多模态输入实体对象
     * @return 插入操作影响的行数
     */
    @Override
    int insert(MultimodalInput multimodalInput);
    
    /**
     * 根据ID更新多模态输入的处理状态
     * 
     * @param id 输入记录ID
     * @param processingStatus 新的处理状态
     * @return 更新操作影响的行数
     */
    @Update("UPDATE multimodal_input SET processing_status = #{processingStatus} WHERE id = #{id}")
    int updateProcessingStatus(@Param("id") Long id, @Param("processingStatus") String processingStatus);
    
    /**
     * 根据用户ID查询该用户的多模态输入历史记录
     * 按时间倒序排列，返回最新的记录在前
     * 
     * @param userId 用户ID
     * @return 用户的多模态输入历史记录列表
     */
    @Select("SELECT * FROM multimodal_input WHERE user_id = #{userId} ORDER BY timestamp DESC")
    List<MultimodalInput> queryHistoryByUser(@Param("userId") Long userId);
    
    /**
     * 根据输入类型和用户ID查询特定类型的输入记录
     * 
     * @param inputType 输入类型（图片、文本、视频）
     * @param userId 用户ID
     * @return 符合条件的多模态输入记录列表
     */
    @Select("SELECT * FROM multimodal_input WHERE input_type = #{inputType} AND user_id = #{userId} ORDER BY timestamp DESC")
    List<MultimodalInput> queryByInputTypeAndUser(@Param("inputType") String inputType, @Param("userId") Long userId);
    
    /**
     * 根据处理状态查询多模态输入记录
     * 
     * @param processingStatus 处理状态
     * @return 指定处理状态的多模态输入记录列表
     */
    @Select("SELECT * FROM multimodal_input WHERE processing_status = #{processingStatus} ORDER BY timestamp ASC")
    List<MultimodalInput> queryByProcessingStatus(@Param("processingStatus") String processingStatus);
    
    /**
     * 批量更新多模态输入记录的处理状态
     * 
     * @param ids 需要更新的记录ID列表
     * @param processingStatus 新的处理状态
     * @return 更新操作影响的行数
     */
    @Update("<script>" +
            "UPDATE multimodal_input SET processing_status = #{processingStatus} WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int batchUpdateProcessingStatus(@Param("ids") List<Long> ids, @Param("processingStatus") String processingStatus);
    
    /**
     * 根据时间范围查询多模态输入记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间范围内的多模态输入记录列表
     */
    @Select("SELECT * FROM multimodal_input WHERE timestamp BETWEEN #{startTime} AND #{endTime} ORDER BY timestamp DESC")
    List<MultimodalInput> queryByTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    
    /**
     * 统计指定用户的多模态输入记录数量
     * 
     * @param userId 用户ID
     * @return 用户的输入记录总数
     */
    @Select("SELECT COUNT(*) FROM multimodal_input WHERE user_id = #{userId}")
    Long countByUser(@Param("userId") Long userId);
}


// 内容由AI生成，仅供参考