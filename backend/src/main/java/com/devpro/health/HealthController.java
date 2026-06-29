package com.devpro.health;

import com.devpro.common.ApiResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /**
     * 健康检查接口，用于前端联调、部署探活和基础运行状态确认。
     *
     * @return 当前服务状态和服务器时间
     */
    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "time", OffsetDateTime.now().toString()
        ));
    }
}
