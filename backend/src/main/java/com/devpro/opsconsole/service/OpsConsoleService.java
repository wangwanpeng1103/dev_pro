package com.devpro.opsconsole.service;

import com.devpro.opsconsole.dto.FunctionNodeRequest;
import com.devpro.opsconsole.dto.ProjectPermissionRequest;
import com.devpro.opsconsole.dto.ProjectRequest;
import com.devpro.opsconsole.dto.UserRequest;
import com.devpro.opsconsole.model.FunctionNode;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.model.UserType;
import com.devpro.opsconsole.repository.OpsConsoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 运维控制台业务服务，负责登录校验、用户项目授权、项目模块和功能树配置。
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

    public List<UserAccount> listUsers() {
        return opsConsoleRepository.findAllUsers();
    }

    public UserAccount createPermanentUser(UserRequest request) {
        return opsConsoleRepository.insertUser(
                request.username(),
                request.displayName(),
                buildDefaultPasswordHash(request.username()),
                UserType.PERMANENT,
                null,
                safeProjectCodes(request.projectCodes())
        );
    }

    public UserAccount createTemporaryUser(UserRequest request) {
        int validHours = request.validHours() == null ? 24 : request.validHours();
        return opsConsoleRepository.insertUser(
                request.username(),
                request.displayName(),
                buildDefaultPasswordHash(request.username()),
                UserType.TEMPORARY,
                LocalDateTime.now().plusHours(validHours),
                safeProjectCodes(request.projectCodes())
        );
    }

    public UserAccount updateUserProjects(String username, ProjectPermissionRequest request) {
        opsConsoleRepository.replaceUserProjects(username, safeProjectCodes(request.projectCodes()));
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

    private String buildDefaultPasswordHash(String username) {
        return "{noop}" + username;
    }

    private List<String> safeProjectCodes(List<String> projectCodes) {
        return CollectionUtils.isEmpty(projectCodes) ? List.of() : projectCodes;
    }
}
