package adapter.controller;

import application.service.SecurityManagementService;
import domain.dto.LoginRequest;
import domain.dto.TokenRequest;
import domain.dto.OperationLogRequest;
import domain.dto.AuditRequest;
import domain.dto.MfaRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

/**
 * Security Management Controller
 * Handles user authentication, permission management, security audit, etc.
 */
@Slf4j
@RestController
@RequestMapping("/api/security")
@Api(tags = "Security Management APIs")
public class SecurityManagementController {

    private final SecurityManagementService securityManagementService;

    /**
     * Constructor injection of service
     */
    @Autowired
    public SecurityManagementController(SecurityManagementService securityManagementService) {
        this.securityManagementService = securityManagementService;
    }

    /**
     * User login authentication and JWT token issuance
     *
     * @param loginRequest user credentials (username and password)
     * @return ResponseEntity containing authentication result and JWT token
     */
    @PostMapping("/login")
    @ApiOperation("User Login Authentication")
    public ResponseEntity<Map<String, Object>> userLoginAuthentication(
            @Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Processing user login request, username: {}", loginRequest.getUsername());
            Map<String, Object> authResult = securityManagementService.implementJWTAuth(loginRequest);
            log.info("User login authentication successful, username: {}", loginRequest.getUsername());
            return ResponseEntity.ok(authResult);
        } catch (IllegalArgumentException e) {
            log.error("User login authentication failed, username: {}, error: {}", 
                     loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Authentication failed", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("User login authentication error, username: {}, error: {}", 
                     loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    /**
     * Issue JWT token (usually combined with login interface, provided here for token refresh)
     *
     * @param tokenRequest token request containing user information
     * @return ResponseEntity containing newly issued JWT token
     */
    @PostMapping("/token")
    @ApiOperation("Issue JWT Token")
    public ResponseEntity<Map<String, String>> issueJWTToken(@Valid @RequestBody TokenRequest tokenRequest) {
        try {
            log.info("Issuing JWT token, user ID: {}", tokenRequest.getUserId());
            
            // Validate user information before token generation
            if (!securityManagementService.validateUserInfo(tokenRequest.getUserInfo())) {
                log.warn("User information validation failed, user ID: {}", tokenRequest.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("error", "User information validation failed"));
            }
            
            String token = securityManagementService.generateToken(tokenRequest.getUserInfo());
            log.info("JWT token issued successfully, user ID: {}", tokenRequest.getUserId());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            log.error("JWT token issuance failed, user ID: {}, error: {}", 
                     tokenRequest.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Token issuance failed", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("JWT token issuance error, user ID: {}, error: {}", 
                     tokenRequest.getUserId(), e.getMessage());
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    /**
     * Role-based access control
     *
     * @param request HTTP request object
     * @param resource requested resource path
     * @return ResponseEntity containing access authorization result
     */
    @GetMapping("/access-control")
    @ApiOperation("Access Permission Control")
    public ResponseEntity<Map<String, Boolean>> roleBasedAccessControl(
            HttpServletRequest request,
            @RequestParam String resource) {
        try {
            String token = extractTokenFromRequest(request);
            String tokenPreview = token.length() > 20 ? token.substring(0, 20) + "..." : token;
            log.info("Starting permission check, resource: {}, Token: {}", resource, tokenPreview);
            
            boolean hasPermission = securityManagementService.roleBasedAccessControl(token, resource);
            log.info("Permission check completed, resource: {}, authorization result: {}", 
                     resource, hasPermission);
            
            return ResponseEntity.ok(Map.of("hasPermission", hasPermission));
        } catch (IllegalArgumentException e) {
            log.error("Token extraction failed, resource: {}, error: {}", resource, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("hasPermission", false));
        } catch (Exception e) {
            log.error("Permission check failed, resource: {}, error: {}", resource, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("hasPermission", false));
        }
    }

    /**
     * Record user operation logs
     *
     * @param operationLogRequest operation log information
     * @return ResponseEntity containing log recording result
     */
    @PostMapping("/operation-log")
    @ApiOperation("Record Operation Log")
    public ResponseEntity<Map<String, String>> recordUserLogs(
            @Valid @RequestBody OperationLogRequest operationLogRequest) {
        try {
            log.info("Recording user operation log, operation type: {}", operationLogRequest.getOperationType());
            securityManagementService.recordOperationLogs(operationLogRequest);
            log.info("User operation log recorded successfully, operation type: {}", 
                     operationLogRequest.getOperationType());
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("User operation log recording failed, operation type: {}, error: {}", 
                     operationLogRequest.getOperationType(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("status", "failed", "message", e.getMessage()));
        }
    }

    /**
     * Perform regular security audit
     *
     * @param auditRequest audit parameters
     * @return ResponseEntity containing audit results
     */
    @PostMapping("/security-audit")
    @ApiOperation("Perform Security Audit")
    public ResponseEntity<Map<String, Object>> performSecurityAudit(
            @Valid @RequestBody AuditRequest auditRequest) {
        try {
            log.info("Performing security audit, audit scope: {}", auditRequest.getAuditScope());
            Map<String, Object> auditResult = securityManagementService.performSecurityAudit(auditRequest);
            log.info("Security audit completed, audit scope: {}", auditRequest.getAuditScope());
            return ResponseEntity.ok(auditResult);
        } catch (Exception e) {
            log.error("Security audit execution failed, audit scope: {}, error: {}", 
                     auditRequest.getAuditScope(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("status", "failed", "message", e.getMessage()));
        }
    }

    /**
     * Support multi-factor authentication
     *
     * @param mfaRequest multi-factor authentication parameters
     * @return ResponseEntity containing MFA verification result
     */
    @PostMapping("/multi-factor-auth")
    @ApiOperation("Multi-Factor Authentication")
    public ResponseEntity<Map<String, Boolean>> supportMultiFactorAuth(
            @Valid @RequestBody MfaRequest mfaRequest) {
        try {
            log.info("Starting multi-factor authentication, user: {}", mfaRequest.getUsername());
            boolean mfaResult = securityManagementService.supportMultiFactorAuth(mfaRequest);
            log.info("Multi-factor authentication completed, user: {}, result: {}", 
                     mfaRequest.getUsername(), mfaResult);
            return ResponseEntity.ok(Map.of("verified", mfaResult));
        } catch (Exception e) {
            log.error("Multi-factor authentication failed, user: {}, error: {}", 
                     mfaRequest.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("verified", false));
        }
    }

    /**
     * Extract JWT token from HTTP request
     *
     * @param request HTTP request object
     * @return extracted JWT token
     * @throws IllegalArgumentException if Authorization header format is invalid
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }
}


// 内容由AI生成，仅供参考