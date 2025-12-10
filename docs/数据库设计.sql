
-- 创建多模态输入记录表
CREATE TABLE multimodal_inputs (
    input_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识输入记录的ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    input_type ENUM('IMAGE', 'TEXT', 'VIDEO') NOT NULL COMMENT '输入类型',
    data_content LONGTEXT NOT NULL COMMENT '输入数据内容（Base64编码或原始文本）',
    processing_status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    location_info JSON COMMENT '解析出的位置信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_status (processing_status)
) COMMENT '存储多模态输入记录';

-- 创建导航路径表
CREATE TABLE navigation_paths (
    path_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识导航路径的ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    venue_id VARCHAR(100) NOT NULL COMMENT '场所ID',
    path_points JSON NOT NULL COMMENT '路径点集合（包含坐标序列）',
    distance_estimate DECIMAL(10,2) NOT NULL COMMENT '预估距离（米）',
    estimated_time INT NOT NULL COMMENT '预估时间（秒）',
    obstacle_info JSON COMMENT '障碍物信息',
    path_status ENUM('ACTIVE', 'EXPIRED', 'UPDATING') NOT NULL DEFAULT 'ACTIVE' COMMENT '路径状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at TIMESTAMP NOT NULL COMMENT '路径过期时间',
    INDEX idx_user_venue (user_id, venue_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_status (path_status)
) COMMENT '存储导航路径信息';

-- 创建模型版本表
CREATE TABLE model_versions (
    version_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识模型版本的ID',
    version_number VARCHAR(50) UNIQUE NOT NULL COMMENT '版本号',
    model_type ENUM('QLoRA_VL', 'NAVIGATION') NOT NULL COMMENT '模型类型',
    training_data_size BIGINT NOT NULL COMMENT '训练数据大小（字节）',
    accuracy_rate DECIMAL(5,4) NOT NULL COMMENT '准确率',
    deployment_status ENUM('TRAINING', 'TESTING', 'DEPLOYED', 'ROLLBACK') NOT NULL COMMENT '部署状态',
    model_path VARCHAR(500) NOT NULL COMMENT '模型文件路径',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deployed_at TIMESTAMP NULL COMMENT '部署时间',
    INDEX idx_version_number (version_number),
    INDEX idx_deployment_status (deployment_status),
    INDEX idx_created_at (created_at)
) COMMENT '存储模型版本信息';

-- 创建知识库表
CREATE TABLE knowledge_bases (
    knowledge_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识知识库记录的ID',
    venue_id VARCHAR(100) NOT NULL COMMENT '场所ID',
    venue_map_data LONGTEXT NOT NULL COMMENT '场所地图数据',
    rule_text TEXT COMMENT '规则文本',
    vector_index MEDIUMBLOB COMMENT '向量索引数据',
    data_source ENUM('CLOUD', 'EDGE') NOT NULL COMMENT '数据来源',
    sync_status ENUM('SYNCED', 'PENDING', 'SYNCING', 'ERROR') NOT NULL DEFAULT 'PENDING' COMMENT '同步状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE KEY uk_venue_source (venue_id, data_source),
    INDEX idx_venue_id (venue_id),
    INDEX idx_sync_status (sync_status),
    INDEX idx_updated_at (updated_at)
) COMMENT '存储知识库数据';

-- 创建资源使用记录表
CREATE TABLE resource_usages (
    usage_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识资源使用记录的ID',
    node_id VARCHAR(100) NOT NULL COMMENT '节点ID',
    gpu_usage_rate DECIMAL(5,4) NOT NULL COMMENT 'GPU使用率',
    memory_usage BIGINT NOT NULL COMMENT '内存使用量（字节）',
    network_traffic BIGINT NOT NULL COMMENT '网络流量（字节）',
    cost_statistics DECIMAL(12,4) NOT NULL COMMENT '成本统计（元）',
    record_date DATE NOT NULL COMMENT '记录日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_node_date (node_id, record_date),
    INDEX idx_record_date (record_date)
) COMMENT '存储资源使用记录';

-- 创建用户角色表
CREATE TABLE user_roles (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识角色的ID',
    role_name VARCHAR(100) UNIQUE NOT NULL COMMENT '角色名称',
    permission_list JSON NOT NULL COMMENT '权限列表',
    user_scope ENUM('SYSTEM', 'TENANT', 'DEPARTMENT') NOT NULL COMMENT '用户范围',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    INDEX idx_role_name (role_name),
    INDEX idx_user_scope (user_scope)
) COMMENT '存储用户角色信息';

-- 创建用户表
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识用户的ID',
    username VARCHAR(100) UNIQUE NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    email VARCHAR(255) UNIQUE NOT NULL COMMENT '邮箱',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    tenant_id VARCHAR(100) COMMENT '租户ID',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    FOREIGN KEY (role_id) REFERENCES user_roles(role_id) ON DELETE RESTRICT,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_is_active (is_active)
) COMMENT '存储用户信息';

-- 创建操作日志表
CREATE TABLE operation_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识日志的ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    operation_type VARCHAR(100) NOT NULL COMMENT '操作类型',
    target_resource VARCHAR(200) NOT NULL COMMENT '目标资源',
    operation_details JSON COMMENT '操作详情',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) COMMENT '存储用户操作日志';

-- 创建数据统计报表表
CREATE TABLE statistical_reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识报表的ID',
    report_type VARCHAR(100) NOT NULL COMMENT '报表类型',
    report_period ENUM('DAILY', 'WEEKLY', 'MONTHLY') NOT NULL COMMENT '报表周期',
    report_data JSON NOT NULL COMMENT '报表数据',
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    period_start DATE NOT NULL COMMENT '统计周期开始',
    period_end DATE NOT NULL COMMENT '统计周期结束',
    INDEX idx_report_type (report_type),
    INDEX idx_period (report_period),
    INDEX idx_generated_at (generated_at),
    INDEX idx_period_range (period_start, period_end)
) COMMENT '存储统计分析报表';

-- 创建触发器：自动更新知识库同步状态历史
DELIMITER //
CREATE TRIGGER tr_knowledge_sync_history 
AFTER UPDATE ON knowledge_bases 
FOR EACH ROW 
BEGIN
    IF OLD.sync_status != NEW.sync_status THEN
        INSERT INTO knowledge_sync_history (knowledge_id, old_status, new_status, changed_at)
        VALUES (NEW.knowledge_id, OLD.sync_status, NEW.sync_status, NOW());
    END IF;
END//
DELIMITER ;

-- 创建视图：活跃用户统计
CREATE VIEW active_users_stats AS
SELECT 
    ur.role_name,
    COUNT(u.user_id) as active_user_count,
    MAX(u.last_login_at) as latest_login
FROM users u
JOIN user_roles ur ON u.role_id = ur.role_id
WHERE u.is_active = TRUE
GROUP BY ur.role_name;

-- 创建存储过程：清理过期导航路径
DELIMITER //
CREATE PROCEDURE sp_cleanup_expired_paths()
BEGIN
    DELETE FROM navigation_paths 
    WHERE expires_at < NOW() AND path_status = 'EXPIRED';
END//
DELIMITER ;


-- 内容由AI生成，仅供参考