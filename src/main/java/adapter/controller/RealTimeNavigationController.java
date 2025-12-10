package adapter.controller;

import application.service.RealTimeNavigationService;
import domain.entity.NavigationPath;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时视频流导航控制器
 * 负责处理实时导航请求，建立视频流连接，持续接收视频帧并返回AR导航指引
 * 支持200+并发会话，监控响应时延确保低于0.8秒，电量低时自动切换省电模式
 */
@Slf4j
@RestController
@RequestMapping("/api/navigation/realtime")
@Api(tags = "实时视频流导航管理")
public class RealTimeNavigationController {

    private static final int MAX_CONCURRENT_SESSIONS = 250;
    private static final double LATENCY_THRESHOLD = 0.8;
    private static final int LOW_BATTERY_THRESHOLD = 20;
    
    // 线程安全的会话状态缓存
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();
    private final Object sessionLock = new Object();

    private final RealTimeNavigationService realTimeNavigationService;

    @Autowired
    public RealTimeNavigationController(RealTimeNavigationService realTimeNavigationService) {
        this.realTimeNavigationService = realTimeNavigationService;
    }

    /**
     * 建立视频流连接
     */
    @PostMapping("/connect")
    @ApiOperation(value = "建立视频流连接", notes = "为用户设备建立实时视频流导航连接")
    public ResponseEntity<Map<String, Object>> establishVideoConnection(
            @ApiParam(value = "用户ID", required = true) @RequestParam @NotNull Long userId,
            @ApiParam(value = "设备信息") @RequestBody @Valid DeviceInfo deviceInfo) {
        
        log.info("开始建立视频流连接，用户ID: {}, 设备型号: {}", userId, deviceInfo.getDeviceModel());
        
        // 原子性检查并发会话数量
        synchronized (sessionLock) {
            int currentSessions = getActiveSessionCount();
            if (currentSessions >= MAX_CONCURRENT_SESSIONS) {
                log.warn("并发会话已达上限，当前会话数: {}, 最大限制: {}", currentSessions, MAX_CONCURRENT_SESSIONS);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "系统繁忙，请稍后重试"));
            }
            
            try {
                String sessionId = realTimeNavigationService.establishVideoConnection(userId, deviceInfo);
                
                // 创建会话状态记录
                SessionState sessionState = new SessionState(sessionId, userId, deviceInfo);
                sessionStates.put(sessionId, sessionState);
                
                // 创建SSE emitter用于实时推送
                SseEmitter emitter = new SseEmitter(30_000L); // 30秒超时
                sessionState.setEmitter(emitter);
                
                Map<String, Object> response = Map.of(
                    "sessionId", sessionId, 
                    "status", "connected",
                    "concurrentSessions", currentSessions + 1,
                    "batteryLevel", deviceInfo.getBatteryLevel(),
                    "powerSavingMode", shouldEnablePowerSaving(deviceInfo.getBatteryLevel())
                );
                
                log.info("视频流连接建立成功，会话ID: {}, 当前并发数: {}", sessionId, currentSessions + 1);
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                log.error("建立视频流连接失败，用户ID: {}", userId, e);
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "连接建立失败: " + e.getMessage()));
            }
        }
    }

    /**
     * 处理视频帧并返回AR导航指引
     */
    @PostMapping("/process-frame")
    @ApiOperation(value = "处理视频帧", notes = "接收视频帧数据并返回实时AR导航指引")
    public ResponseEntity<Map<String, Object>> processVideoFrames(
            @ApiParam(value = "会话ID", required = true) @RequestParam @NotNull String sessionId,
            @ApiParam(value = "视频帧数据") @RequestBody @Valid VideoFrame videoFrame) {
        
        log.debug("处理视频帧，会话ID: {}, 帧大小: {} bytes, 时间戳: {}", 
                 sessionId, videoFrame.getData().length, videoFrame.getTimestamp());
        
        try {
            // 检查会话有效性
            SessionState sessionState = sessionStates.get(sessionId);
            if (sessionState == null || !sessionState.isActive()) {
                log.warn("无效或非活跃的会话ID: {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of("error", "无效的会话ID"));
            }

            // 更新最后活动时间
            sessionState.updateLastActivity();

            NavigationPath navigationPath = realTimeNavigationService.processVideoFrame(sessionId, videoFrame);
            
            // 通过SSE向客户端推送实时导航数据
            pushNavigationUpdate(sessionState, navigationPath);
            
            log.debug("视频帧处理完成，会话ID: {}", sessionId);
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "navigationPath", navigationPath,
                "processingTime", System.currentTimeMillis() - videoFrame.getTimestamp()
            ));
            
        } catch (IllegalArgumentException e) {
            log.warn("处理视频帧参数错误，会话ID: {}", sessionId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("处理视频帧失败，会话ID: {}", sessionId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "视频帧处理失败"));
        }
    }

    /**
     * 获取当前活跃会话数量
     */
    @GetMapping("/sessions/count")
    @ApiOperation(value = "获取并发会话数量", notes = "返回当前活跃的实时导航会话数量")
    public ResponseEntity<Map<String, Object>> getConcurrentSessionStats() {
        int activeCount = getActiveSessionCount();
        int maxSessions = MAX_CONCURRENT_SESSIONS;
        double utilizationRate = (double) activeCount / maxSessions * 100;
        
        Map<String, Object> response = Map.of(
            "activeSessions", activeCount,
            "maxSessions", maxSessions,
            "utilizationRate", String.format("%.1f%%", utilizationRate),
            "availableSessions", maxSessions - activeCount,
            "timestamp", System.currentTimeMillis()
        );
        
        log.debug("查询会话统计: {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * 监控响应时延
     */
    @GetMapping("/monitor/latency")
    @ApiOperation(value = "监控响应时延", notes = "监控指定会话的响应时延，确保低于0.8秒")
    public ResponseEntity<Map<String, Object>> monitorResponseLatency(
            @ApiParam(value = "会话ID", required = true) @RequestParam @NotNull String sessionId) {
        
        log.debug("监控响应时延，会话ID: {}", sessionId);
        
        try {
            SessionState sessionState = sessionStates.get(sessionId);
            if (sessionState == null || !sessionState.isActive()) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的会话ID"));
            }

            Map<String, Object> latencyStats = realTimeNavigationService.getLatencyStatistics(sessionId);
            double processingLatency = (Double) latencyStats.get("currentLatency");
            long networkLatency = calculateNetworkLatency(sessionState);
            
            double totalLatency = processingLatency + networkLatency / 1000.0;
            latencyStats.put("networkLatency", networkLatency);
            latencyStats.put("totalLatency", totalLatency);
            latencyStats.put("threshold", LATENCY_THRESHOLD);
            
            if (totalLatency > LATENCY_THRESHOLD) {
                log.warn("响应时延超过阈值，会话ID: {}, 总时延: {}s, 阈值: {}s", 
                        sessionId, totalLatency, LATENCY_THRESHOLD);
                latencyStats.put("warning", "时延超过阈值");
                // 触发优化措施
                realTimeNavigationService.optimizeLatency(sessionId);
            }
            
            return ResponseEntity.ok(latencyStats);
            
        } catch (IllegalArgumentException e) {
            log.warn("监控时延参数错误，会话ID: {}", sessionId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("监控响应时延失败，会话ID: {}", sessionId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "监控失败"));
        }
    }

    /**
     * 切换省电模式
     */
    @PostMapping("/power-saving")
    @ApiOperation(value = "切换省电模式", notes = "手动切换至省电模式")
    public ResponseEntity<Map<String, Object>> switchPowerSavingMode(
            @ApiParam(value = "会话ID", required = true) @RequestParam @NotNull String sessionId) {
        
        log.info("切换省电模式，会话ID: {}", sessionId);
        
        try {
            SessionState sessionState = sessionStates.get(sessionId);
            if (sessionState == null || !sessionState.isActive()) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的会话ID"));
            }

            realTimeNavigationService.switchToPowerSavingMode(sessionId);
            sessionState.setPowerSavingMode(true);
            
            Map<String, Object> response = Map.of(
                "sessionId", sessionId,
                "mode", "power-saving",
                "message", "已成功切换至省电模式",
                "batteryLevel", sessionState.getDeviceInfo().getBatteryLevel(),
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("省电模式切换成功，会话ID: {}", sessionId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("切换省电模式参数错误，会话ID: {}", sessionId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("切换省电模式失败，会话ID: {}", sessionId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "模式切换失败"));
        }
    }

    /**
     * 关闭视频流连接
     */
    @PostMapping("/disconnect")
    @ApiOperation(value = "关闭视频流连接", notes = "关闭指定的实时导航会话")
    public ResponseEntity<Map<String, Object>> disconnectVideoConnection(
            @ApiParam(value = "会话ID", required = true) @RequestParam @NotNull String sessionId) {
        
        log.info("关闭视频流连接，会话ID: {}", sessionId);
        
        try {
            SessionState sessionState = sessionStates.get(sessionId);
            if (sessionState == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的会话ID"));
            }

            realTimeNavigationService.closeVideoConnection(sessionId);
            cleanupSession(sessionId);
            
            Map<String, Object> response = Map.of(
                "sessionId", sessionId,
                "status", "disconnected",
                "message", "连接已成功关闭",
                "remainingSessions", getActiveSessionCount(),
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("视频流连接关闭成功，会话ID: {}", sessionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("关闭视频流连接失败，会话ID: {}", sessionId, e);
            // 强制清理会话
            cleanupSession(sessionId);
            return ResponseEntity.internalServerError().body(Map.of("error", "连接关闭失败"));
        }
    }

    /**
     * 恢复异常会话
     */
    @PostMapping("/recover")
    @ApiOperation(value = "恢复异常会话", notes = "恢复因异常中断的导航会话")
    public ResponseEntity<Map<String, Object>> recoverSession(
            @ApiParam(value = "会话ID", required = true) @RequestParam @NotNull String sessionId) {
        
        log.info("尝试恢复会话，会话ID: {}", sessionId);
        
        try {
            SessionState sessionState = sessionStates.get(sessionId);
            if (sessionState != null && !sessionState.isActive()) {
                boolean recovered = realTimeNavigationService.recoverSession(sessionId);
                if (recovered) {
                    sessionState.setActive(true);
                    sessionState.updateLastActivity();
                    
                    Map<String, Object> response = Map.of(
                        "sessionId", sessionId,
                        "status", "recovered",
                        "message", "会话恢复成功",
                        "recoveryTime", System.currentTimeMillis()
                    );
                    log.info("会话恢复成功，会话ID: {}", sessionId);
                    return ResponseEntity.ok(response);
                }
            }
            
            Map<String, Object> response = Map.of(
                "sessionId", sessionId,
                "status", "unrecoverable",
                "message", "会话无法恢复，请重新建立连接"
            );
            log.warn("会话无法恢复，会话ID: {}", sessionId);
            return ResponseEntity.status(HttpStatus.GONE).body(response);
            
        } catch (Exception e) {
            log.error("恢复会话失败，会话ID: {}", sessionId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "会话恢复失败"));
        }
    }

    // === 私有辅助方法 ===
    
    private int getActiveSessionCount() {
        return (int) sessionStates.values().stream()
                .filter(SessionState::isActive)
                .count();
    }
    
    private boolean shouldEnablePowerSaving(int batteryLevel) {
        return batteryLevel <= LOW_BATTERY_THRESHOLD;
    }
    
    private long calculateNetworkLatency(SessionState sessionState) {
        // 简化的网络时延计算，实际应根据具体实现调整
        return System.currentTimeMillis() - sessionState.getLastActivityTime();
    }
    
    private void pushNavigationUpdate(SessionState sessionState, NavigationPath navigationPath) {
        try {
            SseEmitter emitter = sessionState.getEmitter();
            if (emitter != null) {
                emitter.send(SseEmitter.event()
                    .name("navigation-update")
                    .data(navigationPath));
            }
        } catch (Exception e) {
            log.warn("推送导航更新失败，会话ID: {}", sessionState.getSessionId(), e);
        }
    }
    
    private void cleanupSession(String sessionId) {
        SessionState sessionState = sessionStates.remove(sessionId);
        if (sessionState != null) {
            try {
                sessionState.getEmitter().complete();
            } catch (Exception e) {
                log.debug("关闭SSE emitter异常，会话ID: {}", sessionId, e);
            }
        }
    }

    /**
     * 会话状态内部类
     */
    private static class SessionState {
        private final String sessionId;
        private final Long userId;
        private final DeviceInfo deviceInfo;
        private SseEmitter emitter;
        private boolean active = true;
        private boolean powerSavingMode;
        private long lastActivityTime;
        
        public SessionState(String sessionId, Long userId, DeviceInfo deviceInfo) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.deviceInfo = deviceInfo;
            this.lastActivityTime = System.currentTimeMillis();
            this.powerSavingMode = shouldEnablePowerSaving(deviceInfo.getBatteryLevel());
        }
        
        public void updateLastActivity() {
            this.lastActivityTime = System.currentTimeMillis();
        }
        
        // Getter and Setter methods
        public String getSessionId() { return sessionId; }
        public Long getUserId() { return userId; }
        public DeviceInfo getDeviceInfo() { return deviceInfo; }
        public SseEmitter getEmitter() { return emitter; }
        public void setEmitter(SseEmitter emitter) { this.emitter = emitter; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public boolean isPowerSavingMode() { return powerSavingMode; }
        public void setPowerSavingMode(boolean powerSavingMode) { this.powerSavingMode = powerSavingMode; }
        public long getLastActivityTime() { return lastActivityTime; }
        
        private static boolean shouldEnablePowerSaving(int batteryLevel) {
            return batteryLevel <= LOW_BATTERY_THRESHOLD;
        }
    }
}

// 临时DTO定义 - 应在独立文件中定义
class DeviceInfo {
    private String deviceModel;
    private int batteryLevel;
    
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }
}

class VideoFrame {
    private byte[] data;
    private long timestamp;
    
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}


// 内容由AI生成，仅供参考