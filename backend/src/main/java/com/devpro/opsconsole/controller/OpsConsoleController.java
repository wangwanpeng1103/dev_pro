package com.devpro.opsconsole.controller;

import com.devpro.common.ApiResponse;
import com.devpro.opsconsole.dto.FunctionNodeRequest;
import com.devpro.opsconsole.dto.ProjectPermissionRequest;
import com.devpro.opsconsole.dto.ProjectRequest;
import com.devpro.opsconsole.dto.UserRequest;
import com.devpro.opsconsole.model.FunctionNode;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.service.OpsConsoleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维控制台管理接口骨架，提供用户、项目模块和功能树配置入口。
 */
@RestController
@RequestMapping("/api/ops-console")
public class OpsConsoleController {

    private final OpsConsoleService opsConsoleService;

    public OpsConsoleController(OpsConsoleService opsConsoleService) {
        this.opsConsoleService = opsConsoleService;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserAccount>> listUsers() {
        return ApiResponse.success(opsConsoleService.listUsers());
    }

    @PostMapping("/users/permanent")
    public ApiResponse<UserAccount> createPermanentUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success(opsConsoleService.createPermanentUser(request));
    }

    @PostMapping("/users/temporary")
    public ApiResponse<UserAccount> createTemporaryUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success(opsConsoleService.createTemporaryUser(request));
    }

    @PutMapping("/users/{username}/projects")
    public ApiResponse<UserAccount> updateUserProjects(
            @PathVariable String username,
            @RequestBody ProjectPermissionRequest request
    ) {
        return ApiResponse.success(opsConsoleService.updateUserProjects(username, request));
    }

    @GetMapping("/projects")
    public ApiResponse<List<ProjectModule>> listProjects(@RequestParam(required = false) String username) {
        if (username == null || username.isBlank()) {
            return ApiResponse.success(opsConsoleService.listAllProjects());
        }
        return ApiResponse.success(opsConsoleService.listProjectsForUser(username));
    }

    @PostMapping("/projects")
    public ApiResponse<ProjectModule> createProject(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(opsConsoleService.createProject(request));
    }

    @GetMapping("/projects/{projectCode}/functions")
    public ApiResponse<List<FunctionNode>> listFunctionNodes(@PathVariable String projectCode) {
        return ApiResponse.success(opsConsoleService.listFunctionNodes(projectCode));
    }

    @PostMapping("/projects/{projectCode}/functions")
    public ApiResponse<FunctionNode> createFunctionNode(
            @PathVariable String projectCode,
            @Valid @RequestBody FunctionNodeRequest request
    ) {
        return ApiResponse.success(opsConsoleService.createFunctionNode(projectCode, request));
    }
}

