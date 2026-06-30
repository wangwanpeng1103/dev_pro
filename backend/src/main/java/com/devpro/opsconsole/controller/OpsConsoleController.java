package com.devpro.opsconsole.controller;

import com.devpro.common.ApiResponse;
import com.devpro.common.PageResult;
import com.devpro.opsconsole.dto.FunctionNodeRequest;
import com.devpro.opsconsole.dto.PermanentUserUpdateRequest;
import com.devpro.opsconsole.dto.ProjectPermissionRequest;
import com.devpro.opsconsole.dto.ProjectRequest;
import com.devpro.opsconsole.dto.UserPasswordUpdateRequest;
import com.devpro.opsconsole.dto.UserRequest;
import com.devpro.opsconsole.dto.UserUpdateRequest;
import com.devpro.opsconsole.model.FunctionNode;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.service.OpsConsoleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维控制台管理接口，提供用户、项目模块和项目功能入口。
 */
@RestController
@RequestMapping("/api/ops-console")
public class OpsConsoleController {

    private final OpsConsoleService opsConsoleService;

    public OpsConsoleController(OpsConsoleService opsConsoleService) {
        this.opsConsoleService = opsConsoleService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<UserAccount>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int pageSize
    ) {
        return ApiResponse.success(opsConsoleService.listUsers(page, pageSize));
    }

    /**
     * 管理员修改指定账号密码，可用于修改自己或其他用户的登录密码。
     *
     * @param username 被修改密码的登录账号
     * @param operatorUsername 操作人登录账号，当前阶段用于校验管理员身份
     * @param request 密码修改请求
     * @return 空响应
     */
    @PutMapping("/users/{username}/password")
    public ApiResponse<Void> updateUserPassword(
            @PathVariable String username,
            @RequestParam String operatorUsername,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        opsConsoleService.updateUserPassword(operatorUsername, username, request);
        return ApiResponse.success(null);
    }

    /**
     * 查询所有永久用户，供用户管理页面维护长期账号。
     *
     * @return 永久用户列表
     */
    @GetMapping("/users/permanent")
    public ApiResponse<List<UserAccount>> listPermanentUsers() {
        return ApiResponse.success(opsConsoleService.listPermanentUsers());
    }

    /**
     * 修改非管理员用户基础信息和项目授权。
     *
     * @param username 登录账号
     * @param request 用户修改请求
     * @return 修改后的用户信息
     */
    @PutMapping("/users/{username}")
    public ApiResponse<UserAccount> updateUser(
            @PathVariable String username,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.success(opsConsoleService.updateUser(username, request));
    }

    /**
     * 删除非管理员用户，并同步清理该用户的项目授权关系。
     *
     * @param username 登录账号
     * @return 空响应
     */
    @DeleteMapping("/users/{username}")
    public ApiResponse<Void> deleteUser(@PathVariable String username) {
        opsConsoleService.deleteUser(username);
        return ApiResponse.success(null);
    }

    /**
     * 创建永久用户，初始密码默认为 1234，后续再接入密码策略。
     *
     * @param request 永久用户创建请求
     * @return 新创建的永久用户
     */
    @PostMapping("/users/permanent")
    public ApiResponse<UserAccount> createPermanentUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success(opsConsoleService.createPermanentUser(request));
    }

    /**
     * 修改永久用户基础信息和项目授权。
     *
     * @param username 登录账号
     * @param request 永久用户修改请求
     * @return 修改后的用户信息
     */
    @PutMapping("/users/permanent/{username}")
    public ApiResponse<UserAccount> updatePermanentUser(
            @PathVariable String username,
            @Valid @RequestBody PermanentUserUpdateRequest request
    ) {
        return ApiResponse.success(opsConsoleService.updatePermanentUser(username, request));
    }

    /**
     * 删除永久用户，并同步清理该用户的项目授权关系。
     *
     * @param username 登录账号
     * @return 空响应
     */
    @DeleteMapping("/users/permanent/{username}")
    public ApiResponse<Void> deletePermanentUser(@PathVariable String username) {
        opsConsoleService.deletePermanentUser(username);
        return ApiResponse.success(null);
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
