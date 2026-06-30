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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    /**
     * 分页查询所有用户（含临时用户），用于用户管理列表展示。
     *
     * @param page     页码，从 1 开始，默认值为 1
     * @param pageSize 每页条数，默认值为 10
     * @return 分页用户列表
     */
    @GetMapping("/users")
    public ApiResponse<PageResult<UserAccount>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
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
    @PostMapping("/users/{username}/password")
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
    @PostMapping("/users/{username}")
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
    @PostMapping("/users/{username}/delete")
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
    @PostMapping("/users/permanent/{username}")
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
    @PostMapping("/users/permanent/{username}/delete")
    public ApiResponse<Void> deletePermanentUser(@PathVariable String username) {
        opsConsoleService.deletePermanentUser(username);
        return ApiResponse.success(null);
    }

    /**
     * 创建临时用户，用于短期访问授权，超过有效期后自动失效。
     *
     * @param request 临时用户创建请求，包含用户名、有效期等必要信息
     * @return 新创建的临时用户信息
     */
    @PostMapping("/users/temporary")
    public ApiResponse<UserAccount> createTemporaryUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success(opsConsoleService.createTemporaryUser(request));
    }

    /**
     * 批量更新用户的项目授权关系，用于为用户分配或收回项目访问权限。
     *
     * @param username 需要修改授权的登录账号
     * @param request  项目授权请求，包含需授权的项目编号列表
     * @return 更新后的用户信息
     */
    @PostMapping("/users/{username}/projects")
    public ApiResponse<UserAccount> updateUserProjects(
            @PathVariable String username,
            @RequestBody ProjectPermissionRequest request
    ) {
        return ApiResponse.success(opsConsoleService.updateUserProjects(username, request));
    }

    /**
     * 查询项目列表，传入用户名时仅返回该用户有权限的项目，否则返回全部项目。
     *
     * @param username 可选参数，指定用户名则按用户权限过滤
     * @return 项目列表
     */
    @GetMapping("/projects")
    public ApiResponse<List<ProjectModule>> listProjects(@RequestParam(required = false) String username) {
        if (username == null || username.isBlank()) {
            return ApiResponse.success(opsConsoleService.listAllProjects());
        }
        return ApiResponse.success(opsConsoleService.listProjectsForUser(username));
    }

    /**
     * 创建新项目，同时初始化项目默认功能入口结构。
     *
     * @param request 项目创建请求，包含项目编号、名称等基本信息
     * @return 新创建的项目信息
     */
    @PostMapping("/projects")
    public ApiResponse<ProjectModule> createProject(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(opsConsoleService.createProject(request));
    }

    /**
     * 查询指定项目下所有功能入口节点，用于构建项目侧边栏导航菜单。
     *
     * @param projectCode 项目编号
     * @return 功能节点树形列表
     */
    @GetMapping("/projects/{projectCode}/functions")
    public ApiResponse<List<FunctionNode>> listFunctionNodes(@PathVariable String projectCode) {
        return ApiResponse.success(opsConsoleService.listFunctionNodes(projectCode));
    }

    /**
     * 在指定项目下创建功能入口节点，支持配置名称、图标、路由路径和父节点层级。
     *
     * @param projectCode 项目编号
     * @param request     功能节点创建请求，包含节点名称、图标、路由及父节点等信息
     * @return 新创建的功能节点
     */
    @PostMapping("/projects/{projectCode}/functions")
    public ApiResponse<FunctionNode> createFunctionNode(
            @PathVariable String projectCode,
            @Valid @RequestBody FunctionNodeRequest request
    ) {
        return ApiResponse.success(opsConsoleService.createFunctionNode(projectCode, request));
    }
}
