package com.devpro.opsconsole.service;

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
import com.devpro.opsconsole.model.UserType;
import com.devpro.opsconsole.repository.OpsConsoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 运维控制台业务服务，负责登录校验、用户项目授权、项目模块和项目功能入口。
 */
@Service
public class OpsConsoleService {

    private final OpsConsoleRepository opsConsoleRepository;

    public OpsConsoleService(OpsConsoleRepository opsConsoleRepository) {
        this.opsConsoleRepository = opsConsoleRepository;
    }

    /**
     * 登录并返回当前用户。第一期使用简单密码校验，后续可替换为 Spring Security。
     *
     * @param username 用户名
     * @param password 密码
     * @return 当前用户
     */
    public UserAccount login(String username, String password) {
        UserAccount userAccount = opsConsoleRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在、已禁用或已过期"));
        if (!userAccount.canLogin(LocalDateTime.now())) {
            throw new IllegalArgumentException("用户不存在、已禁用或已过期");
        }
        String passwordHash = opsConsoleRepository.findPasswordHashByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在、已禁用或已过期"));
        if (!matchesPassword(password, passwordHash)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return userAccount;
    }

    /**
     * 分页查询用户列表，列表中 admin 仍保持第一位，供用户管理页面翻页展示。
     *
     * @param page 当前页码，从 1 开始
     * @param pageSize 每页条数
     * @return 用户分页结果
     */
    public PageResult<UserAccount> listUsers(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(Math.max(1, pageSize), 100);
        return opsConsoleRepository.findUsersPage(safePage, safePageSize);
    }

    /**
     * 管理员修改任意账号密码。当前项目尚未接入会话鉴权，暂通过操作人账号校验管理员身份。
     *
     * @param operatorUsername 操作人登录账号
     * @param targetUsername 被修改密码的登录账号
     * @param request 密码修改请求
     */
    @Transactional
    public void updateUserPassword(String operatorUsername, String targetUsername, UserPasswordUpdateRequest request) {
        ensureAdminUser(operatorUsername);
        opsConsoleRepository.updateUserPassword(targetUsername, buildPasswordHash(request.newPassword()));
    }

    /**
     * 查询永久用户列表，用于用户管理模块的永久账号维护。
     *
     * @return 永久用户列表
     */
    public List<UserAccount> listPermanentUsers() {
        return opsConsoleRepository.findUsersByType(UserType.PERMANENT);
    }

    /**
     * 创建永久用户。当前阶段新增账号默认密码统一为 1234，后续接入密码策略后需要替换这里。
     *
     * @param request 创建请求
     * @return 新创建的永久用户
     */
    @Transactional
    public UserAccount createPermanentUser(UserRequest request) {
        ensureUsernameNotExists(request.username());
        return opsConsoleRepository.insertUser(
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
     *
     * @param username 被修改的登录账号
     * @param request 修改请求
     * @return 修改后的用户信息
     */
    @Transactional
    public UserAccount updateUser(String username, UserUpdateRequest request) {
        opsConsoleRepository.updateUser(
                username,
                request.displayName(),
                request.enabled() == null || request.enabled(),
                assignableProjectCodes(request.projectCodes())
        );
        return opsConsoleRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /**
     * 修改永久用户基础信息和项目授权。
     *
     * @param username 被修改的登录账号
     * @param request 修改请求
     * @return 修改后的用户信息
     */
    @Transactional
    public UserAccount updatePermanentUser(String username, PermanentUserUpdateRequest request) {
        ensurePermanentUser(username);
        opsConsoleRepository.updatePermanentUser(
                username,
                request.displayName(),
                request.enabled() == null || request.enabled(),
                assignableProjectCodes(request.projectCodes())
        );
        return opsConsoleRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /**
     * 删除永久用户及其项目授权关系。
     *
     * @param username 被删除的登录账号
     */
    @Transactional
    public void deletePermanentUser(String username) {
        ensurePermanentUser(username);
        opsConsoleRepository.deleteUser(username);
    }

    /**
     * 删除非管理员用户及其项目授权关系。admin 是系统内置管理员账号，不允许通过列表删除。
     *
     * @param username 被删除的登录账号
     */
    @Transactional
    public void deleteUser(String username) {
        UserAccount userAccount = opsConsoleRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (userAccount.getUserType() == UserType.ADMIN) {
            throw new IllegalArgumentException("不能删除管理员账号");
        }
        opsConsoleRepository.deleteUser(username);
    }

    /**
     * 创建临时用户，并根据有效小时数计算过期时间。
     *
     * @param request 创建请求
     * @return 新创建的临时用户
     */
    @Transactional
    public UserAccount createTemporaryUser(UserRequest request) {
        ensureUsernameNotExists(request.username());
        int validHours = validTemporaryHours(request.validHours());
        return opsConsoleRepository.insertUser(
                request.username(),
                request.displayName(),
                buildDefaultPasswordHash(),
                UserType.TEMPORARY,
                request.enabled() == null || request.enabled(),
                LocalDateTime.now().plusHours(validHours),
                assignableProjectCodes(request.projectCodes())
        );
    }

    /**
     * 替换用户项目授权。当前权限模型只控制项目入口，不控制项目内部功能。
     *
     * @param username 登录账号
     * @param request 项目授权请求
     * @return 更新授权后的用户信息
     */
    @Transactional
    public UserAccount updateUserProjects(String username, ProjectPermissionRequest request) {
        opsConsoleRepository.replaceUserProjects(username, assignableProjectCodes(request.projectCodes()));
        return opsConsoleRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    public List<ProjectModule> listProjectsForUser(String username) {
        UserAccount userAccount = opsConsoleRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return opsConsoleRepository.findProjectsForUser(userAccount);
    }

    public List<ProjectModule> listAllProjects() {
        return opsConsoleRepository.findAllProjects();
    }

    public ProjectModule createProject(ProjectRequest request) {
        return opsConsoleRepository.insertProject(request.code(), request.name(), request.description(), request.iconText());
    }

    public List<FunctionNode> listFunctionNodes(String projectCode) {
        if (opsConsoleRepository.findProjectByCode(projectCode).isEmpty()) {
            throw new IllegalArgumentException("项目模块不存在");
        }
        return opsConsoleRepository.findFunctionNodesByProjectCode(projectCode);
    }

    public FunctionNode createFunctionNode(String projectCode, FunctionNodeRequest request) {
        if (opsConsoleRepository.findProjectByCode(projectCode).isEmpty()) {
            throw new IllegalArgumentException("项目模块不存在");
        }
        FunctionNode functionNode = new FunctionNode(
                null,
                request.parentId(),
                request.code(),
                request.name(),
                request.nodeType(),
                0
        );
        functionNode.setRoutePath(request.routePath());
        functionNode.setExternalUrl(request.externalUrl());
        functionNode.setSsoEnabled(request.ssoEnabled());
        return opsConsoleRepository.insertFunctionNode(projectCode, functionNode);
    }

    private boolean matchesPassword(String rawPassword, String passwordHash) {
        if (passwordHash != null && passwordHash.startsWith("{noop}")) {
            return passwordHash.substring("{noop}".length()).equals(rawPassword);
        }
        return false;
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
        UserAccount userAccount = opsConsoleRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("操作人不存在"));
        if (userAccount.getUserType() != UserType.ADMIN) {
            throw new IllegalArgumentException("只有管理员可以修改用户密码");
        }
    }

    /**
     * 永久用户接口只允许维护普通永久账号，避免误操作 admin 或临时用户。
     */
    private void ensurePermanentUser(String username) {
        UserAccount userAccount = opsConsoleRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (userAccount.getUserType() != UserType.PERMANENT) {
            throw new IllegalArgumentException("只能维护永久用户");
        }
    }

    /**
     * 创建账号前先做业务侧唯一性校验，让前端拿到明确错误提示。
     */
    private void ensureUsernameNotExists(String username) {
        if (opsConsoleRepository.findUserByUsername(username).isPresent()) {
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
