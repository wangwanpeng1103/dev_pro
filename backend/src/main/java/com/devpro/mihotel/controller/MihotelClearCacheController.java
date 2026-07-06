package com.devpro.mihotel.controller;

import com.devpro.common.ApiResponse;
import com.devpro.mihotel.dto.MihotelCacheClearRequest;
import com.devpro.mihotel.dto.MihotelCacheClearResponse;
import com.devpro.mihotel.dto.MihotelCacheTargetResponse;
import com.devpro.mihotel.service.MihotelClearCacheService;
import com.devpro.opsconsole.model.UserType;
import com.devpro.opsconsole.repository.UserAdminRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * mihotel 项目接口，提供缓存清理目标查询和单节点缓存清理能力。
 */
@RestController
@RequestMapping("/api/mihotel")
public class MihotelClearCacheController {

    private final MihotelClearCacheService clearCacheService;
    private final UserAdminRepository userAdminRepository;

    public MihotelClearCacheController(
            MihotelClearCacheService clearCacheService,
            UserAdminRepository userAdminRepository
    ) {
        this.clearCacheService = clearCacheService;
        this.userAdminRepository = userAdminRepository;
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
        if (clearCacheService.isLocalTarget(request.targetCode()) && !isAdmin(request.operatorUsername())) {
            throw new ResponseStatusException(FORBIDDEN, "只有 admin 可以清理本地环境缓存");
        }
        return ApiResponse.success(clearCacheService.clearTarget(request.targetCode()));
    }

    /**
     * 判断当前操作人是否为管理员，用于限制本地环境这类高风险操作。
     *
     * @param operatorUsername 当前操作人账号
     * @return 是否为管理员
     */
    private boolean isAdmin(String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) {
            return false;
        }
        return userAdminRepository.findUserByUsername(operatorUsername.trim())
                .map(operator -> operator.getUserType() == UserType.ADMIN)
                .orElse(false);
    }
}
