package com.navigation.system.domain.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import com.alibaba.fastjson2.JSON;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用户角色领域实体类
 * 表示系统中的角色权限信息，实现基于角色的访问控制(RBAC)
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_role")
public class UserRole {
    
    /**
     * 角色唯一标识符，主键自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 角色名称，唯一且非空
     * 例如：管理员、普通用户、访客等
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;
    
    /**
     * 权限列表，存储该角色拥有的所有权限
     * 使用JSON格式存储权限字符串列表
     * 例如：["NAVIGATION_READ", "MODEL_MANAGE", "KNOWLEDGE_UPDATE"]
     */
    @Column(name = "permission_list", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> permissionList;
    
    /**
     * 用户范围，定义该角色适用的用户类型或部门
     * 例如："ALL_USERS", "ADMIN_DEPARTMENT", "EXTERNAL_USERS"
     */
    @Column(name = "user_scope", nullable = false, length = 100)
    private String userScope;
    
    /**
     * 角色描述，提供详细的角色说明
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 创建时间，记录角色创建的时间戳
     */
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    /**
     * 更新时间，记录角色最后修改的时间戳
     */
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;
    
    /**
     * 是否启用标志，用于软删除功能
     * true-启用，false-禁用
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    /**
     * 实体持久化前的预处理方法
     * 设置创建时间和更新时间，默认启用状态
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdTime = now;
        this.updatedTime = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.permissionList == null) {
            this.permissionList = new ArrayList<>();
        }
    }
    
    /**
     * 实体更新前的预处理方法
     * 更新修改时间戳
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
    
    /**
     * 检查角色是否拥有指定权限
     * 
     * @param permission 要检查的权限字符串
     * @return 如果角色拥有该权限返回true，否则返回false
     */
    public boolean hasPermission(String permission) {
        if (permission == null || this.permissionList == null) {
            return false;
        }
        return this.permissionList.stream()
                .anyMatch(p -> Objects.equals(p, permission));
    }
    
    /**
     * 添加权限到角色
     * 
     * @param permission 要添加的权限字符串
     * @return 如果权限添加成功返回true，如果权限已存在返回false
     */
    public boolean addPermission(String permission) {
        if (permission == null) {
            return false;
        }
        if (this.permissionList == null) {
            this.permissionList = new ArrayList<>();
        }
        if (!this.permissionList.contains(permission)) {
            return this.permissionList.add(permission);
        }
        return false;
    }
    
    /**
     * 从角色中移除权限
     * 
     * @param permission 要移除的权限字符串
     * @return 如果权限移除成功返回true，如果权限不存在返回false
     */
    public boolean removePermission(String permission) {
        if (permission == null || this.permissionList == null) {
            return false;
        }
        return this.permissionList.removeIf(p -> Objects.equals(p, permission));
    }
    
    /**
     * 清空所有权限
     */
    public void clearPermissions() {
        if (this.permissionList != null) {
            this.permissionList.clear();
        }
    }
    
    /**
     * 获取权限数量
     * 
     * @return 权限数量
     */
    public int getPermissionCount() {
        return this.permissionList == null ? 0 : this.permissionList.size();
    }
    
    /**
     * 检查角色是否启用
     * 
     * @return 如果角色启用返回true，否则返回false
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.isActive);
    }
    
    /**
     * 字符串列表转换器
     * 用于将List<String>与数据库中的JSON字符串相互转换
     */
    @Converter
    public static class StringListConverter implements AttributeConverter<List<String>, String> {
        
        @Override
        public String convertToDatabaseColumn(List<String> attribute) {
            return attribute == null || attribute.isEmpty() ? "[]" : JSON.toJSONString(attribute);
        }
        
        @Override
        public List<String> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return new ArrayList<>();
            }
            try {
                return JSON.parseArray(dbData, String.class);
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
    }
}


// 内容由AI生成，仅供参考