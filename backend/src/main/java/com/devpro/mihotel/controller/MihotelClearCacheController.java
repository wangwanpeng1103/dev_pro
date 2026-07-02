package com.devpro.mihotel.controller;

import com.devpro.common.ApiResponse;
import com.devpro.mihotel.dto.MihotelCacheClearRequest;
import com.devpro.mihotel.dto.MihotelCacheClearResponse;
import com.devpro.mihotel.dto.MihotelCacheTargetResponse;
import com.devpro.mihotel.service.MihotelClearCacheService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * mihotel 项目接口，提供缓存清理目标查询和单节点缓存清理能力。
 */
@RestController
@RequestMapping("/api/mihotel")
public class MihotelClearCacheController {

    private final MihotelClearCacheService clearCacheService;

    public MihotelClearCacheController(MihotelClearCacheService clearCacheService) {
        this.clearCacheService = clearCacheService;
    }

    /**
     * 查询 mihotel 可清理的缓存目标，真实地址仅保存在后端配置中。
     *
     * @return 缓存清理目标列表
     */
    @GetMapping("/cache-targets")
    public ApiResponse<List<MihotelCacheTargetResponse>> listCacheTargets() {
        return ApiResponse.success(clearCacheService.listTargets());
    }

    /**
     * 清理指定 mihotel 服务节点缓存；主干多节点顺序控制由前端按本接口逐个调用。
     *
     * @param request 清理请求
     * @return 单节点清理结果
     */
    @PostMapping("/clear-cache")
    public ApiResponse<MihotelCacheClearResponse> clearCache(@Valid @RequestBody MihotelCacheClearRequest request) {
        return ApiResponse.success(clearCacheService.clearTarget(request.targetCode()));
    }
}
