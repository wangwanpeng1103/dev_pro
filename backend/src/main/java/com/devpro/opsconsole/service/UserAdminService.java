package com.devpro.opsconsole.service;

import com.devpro.common.PageResult;
import com.devpro.opsconsole.dto.UserPasswordUpdateRequest;
import com.devpro.opsconsole.dto.UserRequest;
import com.devpro.opsconsole.dto.UserUpdateRequest;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.model.UserType;
import com.devpro.opsconsole.repository.UserAdminRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 用户管理业务服务，承载用户列表、账号维护、密码维护和项目授权等用户管理功能。
 */
@Service
public class UserAdminService {

    private final UserAdminRepository userAdminRepository;

    public UserAdminService(UserAdminRepository userAdminRepository) {
        this.userAdminRepository = userAdminRepository;
    }

    /**
     * 分页查询用户列表，列表中 admin 仍保持第一位，供用户管理页面翻页展示。
     */
    public PageResult<UserAccount> listUsers(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(Math.max(1, pageSize), 100);
        return userAdminRepository.findUsersPage(safePage, safePageSize);
    }

    /**
     * 管理员修改任意账号密码。当前项目尚未接入会话鉴权，暂通过操作人账号校验管理员身份。
     */
    @Transactional
    public void updateUserPassword(String operatorUsername, String targetUsername, UserPasswordUpdateRequest request) {
        ensureAdminUser(operatorUsername);
        userAdminRepository.updateUserPassword(targetUsername, buildPasswordHash(request.newPassword()));
    }

    /**
     * 创建永久用户。当前阶段新增账号默认密码统一为 1234，后续接入密码策略后需要替换这里。
     */
    @Transactional
    public UserAccount createPermanentUser(UserRequest request) {
        ensureUsernameNotExists(request.username());
        return userAdminRepository.insertUser(
                request.username(),
                request.displayName(),
                buildDefaultPasswordHash(),
                UserType.PERMANENT,
                request.enabled() == null || request.enabled(),
                null,
                assignableProjectCodes(request.projectCodes())
        );
    }

    /**
     * 修改非管理员用户基础信息和项目授权，用户类型和账号不允许在修改时变更。
     */
    @Transactional
    public UserAccount updateUser(String username, UserUpdateRequest request) {
        userAdminRepository.updateUser(
                username,
                request.displayName(),
                request.enabled() == null || request.enabled(),
                assignableProjectCodes(request.projectCodes())
        );
        return userAdminRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /**
     * 删除非管理员用户及其项目授权关系。admin 是系统内置管理员账号，不允许通过列表删除。
     */
    @Transactional
    public void deleteUser(String username) {
        UserAccount userAccount = userAdminRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (userAccount.getUserType() == UserType.ADMIN) {
            throw new IllegalArgumentException("不能删除管理员账号");
        }
        userAdminRepository.deleteUser(username);
    }

    /**
     * 创建临时用户，并根据有效小时数计算过期时间。
     */
    @Transactional
    public UserAccount createTemporaryUser(UserRequest request) {
        ensureUsernameNotExists(request.username());
        int validHours = validTemporaryHours(request.validHours());
        return userAdminRepository.insertUser(
                request.username(),
                request.displayName(),
                buildDefaultPasswordHash(),
                UserType.TEMPORARY,
                request.enabled() == null || request.enabled(),
                LocalDateTime.now().plusHours(validHours),
                assignableProjectCodes(request.projectCodes())
        );
    }

    private String buildDefaultPasswordHash() {
        return buildPasswordHash("1234");
    }

    private String buildPasswordHash(String rawPassword) {
        return "{noop}" + rawPassword;
    }

    /**
     * 校验操作人必须是管理员，避免普通用户调用管理接口修改他人密码。
     */
    private void ensureAdminUser(String username) {
        UserAccount userAccount = userAdminRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("操作人不存在"));
        if (userAccount.getUserType() != UserType.ADMIN) {
            throw new IllegalArgumentException("只有管理员可以修改用户密码");
        }
    }

    /**
     * 创建账号前先做业务侧唯一性校验，让前端拿到明确错误提示。
     */
    private void ensureUsernameNotExists(String username) {
        if (userAdminRepository.findUserByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户已存在");
        }
    }

    private List<String> safeProjectCodes(List<String> projectCodes) {
        return CollectionUtils.isEmpty(projectCodes) ? List.of() : projectCodes;
    }

    /**
     * 临时用户有效期必须由前端明确传入正整数小时数，用于叠加当前时间计算过期时间。
     */
    private int validTemporaryHours(Integer validHours) {
        if (validHours == null || validHours <= 0) {
            throw new IllegalArgumentException("临时用户可用时间必须是正整数小时");
        }
        return validHours;
    }

    /**
     * 普通账号不能被分配用户管理入口，该入口当前仅由 admin 管理员账号独有。
     */
    private List<String> assignableProjectCodes(List<String> projectCodes) {
        return safeProjectCodes(projectCodes).stream()
                .filter(projectCode -> !"user-admin".equals(projectCode))
                .toList();
    }
}
