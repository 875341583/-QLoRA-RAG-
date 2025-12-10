package infrastructure.sql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import domain.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Knowledge base data mapper interface.
 * Responsible for database operations of knowledge base data, including inserting knowledge data,
 * querying vector indexes, updating synchronization status, etc.
 *
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * Synchronization status constants.
     */
    String SYNC_STATUS_PENDING = "PENDING";
    String SYNC_STATUS_SYNCING = "SYNCING";
    String SYNC_STATUS_SYNCED = "SYNCED";
    String SYNC_STATUS_FAILED = "FAILED";

    /**
     * Inserts a knowledge base record.
     *
     * @param knowledgeBase knowledge base entity object
     * @return number of affected rows
     */
    @Override
    int insert(KnowledgeBase knowledgeBase);

    /**
     * Queries knowledge base record by ID.
     *
     * @param id knowledge base record ID
     * @return knowledge base entity object
     */
    @Select("SELECT * FROM knowledge_base WHERE id = #{id} AND deleted = 0")
    KnowledgeBase selectById(@Param("id") Long id);

    /**
     * Queries the latest vector index for specified venue.
     *
     * @param venueId venue ID
     * @return vector index byte array (compatible with FAISS binary format)
     */
    @Select("SELECT vector_index FROM knowledge_base WHERE venue_id = #{venueId} "
            + "AND deleted = 0 ORDER BY update_time DESC LIMIT 1")
    byte[] queryVectorIndex(@Param("venueId") Long venueId);

    /**
     * Updates knowledge base synchronization status.
     *
     * @param id knowledge base record ID
     * @param syncStatus synchronization status
     * @return number of affected rows
     */
    @Update("UPDATE knowledge_base SET sync_status = #{syncStatus}, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0")
    int updateSyncStatus(@Param("id") Long id, @Param("syncStatus") String syncStatus);

    /**
     * Queries knowledge base records pending synchronization.
     *
     * @return list of knowledge base records with PENDING sync status
     */
    @Select("SELECT * FROM knowledge_base WHERE sync_status = '" + SYNC_STATUS_PENDING + "' AND deleted = 0 "
            + "ORDER BY update_time ASC")
    List<KnowledgeBase> selectPendingSyncRecords();

    /**
     * Queries knowledge base records by update time range.
     *
     * @param startTime start time (format: YYYY-MM-DD HH:mm:ss)
     * @param endTime end time (format: YYYY-MM-DD HH:mm:ss)
     * @return list of knowledge base records within specified time range
     */
    @Select("SELECT * FROM knowledge_base WHERE update_time BETWEEN #{startTime} AND #{endTime} "
            + "AND deleted = 0 ORDER BY update_time DESC")
    List<KnowledgeBase> selectByUpdateTimeRange(@Param("startTime") String startTime,
                                                @Param("endTime") String endTime);

    /**
     * Logically deletes a knowledge base record.
     *
     * @param id knowledge base record ID
     * @return number of affected rows
     */
    @Update("UPDATE knowledge_base SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    int logicalDelete(@Param("id") Long id);

    /**
     * Restores a logically deleted knowledge base record.
     *
     * @param id knowledge base record ID
     * @return number of affected rows
     */
    @Update("UPDATE knowledge_base SET deleted = 0, update_time = NOW() WHERE id = #{id}")
    int restoreLogicalDelete(@Param("id") Long id);

    /**
     * Queries the latest knowledge base record by venue ID.
     *
     * @param venueId venue ID
     * @return latest knowledge base entity object
     */
    @Select("SELECT * FROM knowledge_base WHERE venue_id = #{venueId} AND deleted = 0 "
            + "ORDER BY update_time DESC LIMIT 1")
    KnowledgeBase selectLatestByVenueId(@Param("venueId") Long venueId);

    /**
     * Batch updates synchronization status.
     *
     * @param ids list of knowledge base record IDs
     * @param syncStatus synchronization status
     * @return number of affected rows
     */
    @Update({"<script>",
            "UPDATE knowledge_base SET sync_status = #{syncStatus}, update_time = NOW() ",
            "WHERE id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            " AND deleted = 0",
            "</script>"})
    int batchUpdateSyncStatus(@Param("ids") List<Long> ids, @Param("syncStatus") String syncStatus);

    /**
     * Queries knowledge base records by venue ID.
     *
     * @param venueId venue ID
     * @return list of knowledge base records for specified venue
     */
    @Select("SELECT * FROM knowledge_base WHERE venue_id = #{venueId} AND deleted = 0 "
            + "ORDER BY update_time DESC")
    List<KnowledgeBase> selectByVenueId(@Param("venueId") Long venueId);

    /**
     * Queries knowledge base records by synchronization status.
     *
     * @param syncStatus synchronization status
     * @return list of knowledge base records with specified synchronization status
     */
    @Select("SELECT * FROM knowledge_base WHERE sync_status = #{syncStatus} AND deleted = 0 "
            + "ORDER BY update_time DESC")
    List<KnowledgeBase> selectBySyncStatus(@Param("syncStatus") String syncStatus);

    /**
     * Updates node synchronization timestamp.
     *
     * @param id knowledge base record ID
     * @param nodeType node type (CLOUD/EDGE)
     * @return number of affected rows
     */
    @Update("UPDATE knowledge_base SET ${nodeType}_sync_time = NOW() WHERE id = #{id} AND deleted = 0")
    int updateNodeSyncTime(@Param("id") Long id, @Param("nodeType") String nodeType);

    /**
     * Queries knowledge base records for backup within time range.
     *
     * @param startTime start time
     * @param endTime end time
     * @return list of knowledge base records for backup
     */
    @Select("SELECT * FROM knowledge_base WHERE update_time BETWEEN #{startTime} AND #{endTime} "
            + "AND deleted = 0 ORDER BY update_time DESC")
    List<KnowledgeBase> selectForBackup(@Param("startTime") String startTime, 
                                        @Param("endTime") String endTime);

    /**
     * Inserts backup record into backup table.
     *
     * @param knowledgeBase knowledge base entity object
     * @return number of affected rows
     */
    @Update("INSERT INTO knowledge_base_backup (id, venue_map_data, rule_text, vector_index, "
            + "sync_status, create_time, update_time, deleted) "
            + "VALUES (#{kb.id}, #{kb.venueMapData}, #{kb.ruleText}, #{kb.vectorIndex}, "
            + "#{kb.syncStatus}, #{kb.createTime}, #{kb.updateTime}, #{kb.deleted})")
    int insertBackupRecord(@Param("kb") KnowledgeBase knowledgeBase);

    /**
     * Queries knowledge base backup records by ID.
     *
     * @param id backup record ID
     * @return knowledge base backup entity object
     */
    @Select("SELECT * FROM knowledge_base_backup WHERE id = #{id} ORDER BY update_time DESC LIMIT 1")
    KnowledgeBase selectBackupById(@Param("id") Long id);
}


// 内容由AI生成，仅供参考