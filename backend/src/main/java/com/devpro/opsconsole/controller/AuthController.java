package com.devpro.opsconsole.controller;

import com.devpro.common.ApiResponse;
import com.devpro.opsconsole.dto.LoginRequest;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.service.OpsConsoleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录接口骨架，后续接入正式认证、密码校验和会话管理。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OpsConsoleService opsConsoleService;

    public AuthController(OpsConsoleService opsConsoleService) {
        this.opsConsoleService = opsConsoleService;
    }

    /**
     * 用户登录，返回用户信息和当前可访问项目模块。
     *
     * @param request 登录请求
     * @return 登录上下文
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        UserAccount userAccount = opsConsoleService.login(request.username(), request.password());
        List<ProjectModule> projects = opsConsoleService.listProjectsForUser(userAccount.getUsername());
        return ApiResponse.success(Map.of(
                "user", userAccount,
                "projects", projects
        ));
    }
}
