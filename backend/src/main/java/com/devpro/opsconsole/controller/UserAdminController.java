package com.devpro.opsconsole.controller;

import com.devpro.common.ApiResponse;
import com.devpro.common.PageResult;
import com.devpro.opsconsole.dto.TemporaryUserTimeExtendRequest;
import com.devpro.opsconsole.dto.UserPasswordUpdateRequest;
import com.devpro.opsconsole.dto.UserRequest;
import com.devpro.opsconsole.dto.UserUpdateRequest;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理项目接口，提供用户列表、账号维护、密码维护和项目授权能力。
 */
@RestController
@RequestMapping("/api/user-admin")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    /**
     * 分页查询所有用户，包含 admin 和临时用户，用于用户管理列表展示。
     *
     * @param page 页码，从 1 开始，默认值为 1
     * @param pageSize 每页条数，默认值为 10
     * @return 分页用户列表
     */
    @GetMapping("/users")
    public ApiResponse<PageResult<UserAccount>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(userAdminService.listUsers(page, pageSize));
    }

    /**
     * 管理员修改指定账号密码，可用于修改自己或其他用户的登录密码。
     */
    @PostMapping("/users/{username}/password")
    public ApiResponse<Void> updateUserPassword(
            @PathVariable String username,
            @RequestParam String operatorUsername,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        userAdminService.updateUserPassword(operatorUsername, username, request);
        return ApiResponse.success(null);
    }

    /**
     * 修改非管理员用户基础信息和项目授权。
     */
    @PostMapping("/users/{username}")
    public ApiResponse<UserAccount> updateUser(
            @PathVariable String username,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.success(userAdminService.updateUser(username, request));
    }

    /**
     * 删除非管理员用户，并同步清理该用户的项目授权关系。
     */
    @PostMapping("/users/{username}/delete")
    public ApiResponse<Void> deleteUser(@PathVariable String username) {
        userAdminService.deleteUser(username);
        return ApiResponse.success(null);
    }

    /**
     * 创建永久用户，初始密码默认值为 1234。
     */
    @PostMapping("/users/permanent")
    public ApiResponse<UserAccount> createPermanentUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success(userAdminService.createPermanentUser(request));
    }

    /**
     * 创建临时用户，用于短期访问授权，超过有效期后自动失效。
     */
    @PostMapping("/users/temporary")
    public ApiResponse<UserAccount> createTemporaryUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success(userAdminService.createTemporaryUser(request));
    }

    /**
     * 给临时用户快速增加有效时间，仅允许操作临时用户账号。
     */
    @PostMapping("/users/{username}/temporary-time")
    public ApiResponse<UserAccount> extendTemporaryUserTime(
            @PathVariable String username,
            @Valid @RequestBody TemporaryUserTimeExtendRequest request
    ) {
        return ApiResponse.success(userAdminService.extendTemporaryUserTime(username, request));
    }
}
