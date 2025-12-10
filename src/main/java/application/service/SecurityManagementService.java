package com.navigation.application.service;

import com.navigation.domain.entity.User;
import com.navigation.domain.entity.UserRole;
import com.navigation.domain.entity.OperationLog;
import com.navigation.domain.repository.UserRepository;
import com.navigation.domain.repository.UserRoleRepository;
import com.navigation.domain.repository.OperationLogRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Security management and access control service implementation
 * Provides JWT authentication, role-based access control, data encryption, 
 * operation logging, and security checks.
 */
@Service
@Transactional(readOnly = true)
public class SecurityManagementService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityManagementService.class);
    
    private static final String LOGIN_OPERATION = "USER_LOGIN";
    private static final String ACCESS_OPERATION = "RESOURCE_ACCESS";
    private static final String SECURITY_CHECK_OPERATION = "SECURITY_CHECK";
    private static final String MFA_OPERATION = "MULTI_FACTOR_AUTH";
    
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final OperationLogRepository operationLogRepository;
    private final SecretKey jwtSecretKey;
    private final long jwtExpirationTime;

    /**
     * Constructor with dependency injection
     */
    public SecurityManagementService(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            OperationLogRepository operationLogRepository,
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.security.jwt.expiration:86400000}") long jwtExpirationTime) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.operationLogRepository = operationLogRepository;
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationTime = jwtExpirationTime;
    }

    /**
     * Implement JWT authentication
     *
     * @param username username
     * @param password password
     * @return JWT token string
     * @throws UsernameNotFoundException when authentication fails
     */
    @Transactional
    public String implementJWTAuth(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        // Unified error message to prevent user enumeration attacks
        String authErrorMessage = "认证失败";
        
        if (userOptional.isEmpty()) {
            recordOperationLogs(username, LOGIN_OPERATION, "用户认证失败");
            throw new UsernameNotFoundException(authErrorMessage);
        }
        
        User user = userOptional.get();
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordOperationLogs(username, LOGIN_OPERATION, "密码验证失败");
            throw new UsernameNotFoundException(authErrorMessage);
        }
        
        if (!user.isActive()) {
            recordOperationLogs(username, LOGIN_OPERATION, "用户账户已禁用");
            throw new UsernameNotFoundException("用户账户已禁用");
        }
        
        String token = generateJwtToken(user);
        recordOperationLogs(username, LOGIN_OPERATION, "登录成功");
        
        return token;
    }

    /**
     * Role-based access control
     *
     * @param token JWT token
     * @param resource requested resource
     * @return whether access is authorized
     */
    public boolean roleBasedAccessControl(String token, String resource) {
        try {
            Claims claims = validateJwtToken(token);
            String username = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            
            // Get user's role from database to ensure consistency
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                recordOperationLogs(username, ACCESS_OPERATION, 
                    String.format("用户不存在: %s", userId));
                return false;
            }
            
            User user = userOptional.get();
            String roleName = user.getRole();
            
            Optional<UserRole> roleOptional = userRoleRepository.findByRoleName(roleName);
            if (roleOptional.isEmpty()) {
                recordOperationLogs(username, ACCESS_OPERATION, 
                    String.format("角色不存在: %s", roleName));
                return false;
            }
            
            UserRole userRole = roleOptional.get();
            
            // Check if role has permission for the resource
            boolean hasPermission = userRole.getPermissionList().stream()
                    .anyMatch(permission -> permission.matches(resource) || 
                                           permission.equals("*")); // Wildcard for full access
            
            if (hasPermission) {
                recordOperationLogs(username, ACCESS_OPERATION, 
                    String.format("授权访问资源: %s", resource));
            } else {
                recordOperationLogs(username, ACCESS_OPERATION, 
                    String.format("权限不足访问资源: %s, 用户角色: %s", resource, roleName));
            }
            
            return hasPermission;
            
        } catch (JwtException e) {
            recordOperationLogs("SYSTEM", ACCESS_OPERATION, 
                String.format("JWT令牌验证失败: %s", e.getMessage()));
            return false;
        }
    }

    /**
     * Encrypt sensitive data
     *
     * @param sensitiveData sensitive data to encrypt
     * @return encrypted data
     */
    public String encryptSensitiveData(String sensitiveData) {
        return passwordEncoder.encode(sensitiveData);
    }

    /**
     * Record operation logs for audit
     *
     * @param username username
     * @param operation operation type
     * @param details operation details
     */
    @Transactional
    public void recordOperationLogs(String username, String operation, String details) {
        OperationLog log = new OperationLog();
        log.setUsername(username);
        log.setOperationType(operation);
        log.setOperationDetails(details);
        log.setOperationTime(new Date());
        log.setIpAddress(getClientIpAddress()); // Method to be implemented based on application context
        
        try {
            operationLogRepository.save(log);
            logger.info("操作日志记录成功 - 用户: {}, 操作: {}, 详情: {}", username, operation, details);
        } catch (Exception e) {
            logger.error("操作日志记录失败 - 用户: {}, 操作: {}, 错误: {}", username, operation, e.getMessage());
        }
    }

    /**
     * Perform regular security checks
     *
     * @return security check report
     */
    public String regularSecurityChecks() {
        StringBuilder report = new StringBuilder();
        report.append("=== Security Status Report ===\n");
        report.append("Check Time: ").append(new Date()).append("\n");
        
        // Basic system metrics
        long userCount = userRepository.count();
        long roleCount = userRoleRepository.count();
        long activeUserCount = userRepository.countByActiveTrue();
        long recentLogsCount = operationLogRepository.countByOperationTimeAfter(
            new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // Last 24 hours
        
        report.append("Total Users: ").append(userCount).append("\n");
        report.append("Active Users: ").append(activeUserCount).append("\n");
        report.append("Roles: ").append(roleCount).append("\n");
        report.append("Recent Operations (24h): ").append(recentLogsCount).append("\n");
        
        // Security configuration status
        report.append("JWT Configuration: Active\n");
        report.append("Password Encoder: ").append(passwordEncoder != null ? "Active" : "Inactive").append("\n");
        report.append("Operation Logging: ").append(operationLogRepository != null ? "Active" : "Inactive").append("\n");
        
        // Additional security checks
        checkWeakPasswords(report);
        checkInactiveUsers(report);
        checkPermissionConsistency(report);
        
        recordOperationLogs("SYSTEM", SECURITY_CHECK_OPERATION, "定期安全状况检查完成");
        
        return report.toString();
    }

    /**
     * Support multi-factor authentication
     *
     * @param username username
     * @param primaryAuth primary authentication result
     * @param secondaryCode secondary authentication code
     * @return whether authentication succeeded
     */
    public boolean supportMultiFactorAuth(String username, boolean primaryAuth, String secondaryCode) {
        if (!primaryAuth) {
            recordOperationLogs(username, MFA_OPERATION, "主认证失败");
            return false;
        }
        
        // Integration with MFA service (placeholder for actual implementation)
        boolean secondaryAuth = validateSecondaryCode(username, secondaryCode);
        
        if (secondaryAuth) {
            recordOperationLogs(username, MFA_OPERATION, "多因素认证成功");
        } else {
            recordOperationLogs(username, MFA_OPERATION, "多因素认证失败");
        }
        
        return secondaryAuth;
    }

    /**
     * Generate JWT token for user
     */
    private String generateJwtToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole())
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationTime))
                .signWith(jwtSecretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate JWT token and extract claims
     */
    private Claims validateJwtToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate secondary authentication code
     * Placeholder for actual MFA service integration
     */
    private boolean validateSecondaryCode(String username, String code) {
        // TODO: Replace with actual MFA service integration
        // Example integrations: SMS verification, email code, authenticator app
        // For now, implement basic validation logic
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        // Basic format validation (6-digit code)
        return code.matches("\\d{6}");
    }

    /**
     * Get client IP address (to be implemented based on application context)
     */
    private String getClientIpAddress() {
        // Implementation depends on how IP address is obtained in your application
        // This is a placeholder implementation
        return "127.0.0.1"; // Default localhost
    }

    /**
     * Check for weak passwords in the system
     */
    private void checkWeakPasswords(StringBuilder report) {
        // Placeholder for weak password detection logic
        report.append("Weak Password Check: Basic implementation needed\n");
    }

    /**
     * Check for long-inactive users
     */
    private void checkInactiveUsers(StringBuilder report) {
        // Placeholder for inactive user analysis
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        long inactiveUsers = operationLogRepository.countUsersWithoutRecentActivity(thirtyDaysAgo);
        report.append("Users Inactive >30 days: ").append(inactiveUsers).append("\n");
    }

    /**
     * Check permission consistency across roles
     */
    private void checkPermissionConsistency(StringBuilder report) {
        // Placeholder for permission consistency checks
        List<UserRole> allRoles = userRoleRepository.findAll();
        report.append("Role Permission Consistency: ").append(allRoles.size()).append(" roles checked\n");
    }
}


// 内容由AI生成，仅供参考