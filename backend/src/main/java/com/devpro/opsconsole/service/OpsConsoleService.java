package com.devpro.opsconsole.service;

import com.devpro.opsconsole.dto.FunctionNodeRequest;
import com.devpro.opsconsole.dto.ProjectPermissionRequest;
import com.devpro.opsconsole.dto.ProjectRequest;
import com.devpro.opsconsole.dto.UserRequest;
import com.devpro.opsconsole.model.FunctionNode;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.model.UserType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 运维控制台骨架服务，当前用内存数据表达产品模型，后续替换为数据库持久化实现。
 */
@Service
public class OpsConsoleService {

    private final AtomicLong userIdGenerator = new AtomicLong(1);
    private final AtomicLong projectIdGenerator = new AtomicLong(1);
    private final AtomicLong functionIdGenerator = new AtomicLong(1);
    private final Map<String, UserAccount> users = new LinkedHashMap<>();
    private final Map<String, ProjectModule> projects = new LinkedHashMap<>();

    public OpsConsoleService() {
        initProjects();
        initUsers();
    }

    /**
     * 登录并返回当前用户可访问项目，第一期仅做骨架校验，不落真实会话。
     *
     * @param username 用户名
     * @return 当前用户
     */
    public UserAccount login(String username) {
        UserAccount userAccount = users.get(username);
        if (userAccount == null || !userAccount.canLogin(LocalDateTime.now())) {
            throw new IllegalArgumentException("用户不存在、已禁用或已过期");
        }
        return userAccount;
    }

    public List<UserAccount> listUsers() {
        return users.values().stream()
                .sorted(Comparator.comparing(UserAccount::getId))
                .toList();
    }

    public UserAccount createPermanentUser(UserRequest request) {
        UserAccount userAccount = new UserAccount(
                userIdGenerator.getAndIncrement(),
                request.username(),
                request.displayName(),
                UserType.PERMANENT,
                null
        );
        grantProjects(userAccount, request.projectCodes());
        users.put(userAccount.getUsername(), userAccount);
        return userAccount;
    }

    public UserAccount createTemporaryUser(UserRequest request) {
        int validHours = request.validHours() == null ? 24 : request.validHours();
        UserAccount userAccount = new UserAccount(
                userIdGenerator.getAndIncrement(),
                request.username(),
                request.displayName(),
                UserType.TEMPORARY,
                LocalDateTime.now().plusHours(validHours)
        );
        grantProjects(userAccount, request.projectCodes());
        users.put(userAccount.getUsername(), userAccount);
        return userAccount;
    }

    public UserAccount updateUserProjects(String username, ProjectPermissionRequest request) {
        UserAccount userAccount = requireUser(username);
        userAccount.getProjectCodes().clear();
        grantProjects(userAccount, request.projectCodes());
        return userAccount;
    }

    public List<ProjectModule> listProjectsForUser(String username) {
        UserAccount userAccount = requireUser(username);
        if (userAccount.getUserType() == UserType.ADMIN) {
            return sortedProjects(projects.values().stream().toList());
        }
        return sortedProjects(projects.values().stream()
                .filter(project -> userAccount.getProjectCodes().contains(project.getCode()))
                .filter(ProjectModule::isEnabled)
                .toList());
    }

    public List<ProjectModule> listAllProjects() {
        return sortedProjects(new ArrayList<>(projects.values()));
    }

    public ProjectModule createProject(ProjectRequest request) {
        ProjectModule projectModule = new ProjectModule(
                projectIdGenerator.getAndIncrement(),
                request.code(),
                request.name(),
                request.description(),
                request.iconText(),
                projects.size() * 10 + 10
        );
        projects.put(projectModule.getCode(), projectModule);
        users.values().stream()
                .filter(user -> user.getUserType() == UserType.ADMIN)
                .forEach(user -> user.getProjectCodes().add(projectModule.getCode()));
        return projectModule;
    }

    public List<FunctionNode> listFunctionNodes(String projectCode) {
        return requireProject(projectCode).getFunctionNodes().stream()
                .filter(FunctionNode::isEnabled)
                .sorted(Comparator.comparing(FunctionNode::getSortOrder))
                .toList();
    }

    public FunctionNode createFunctionNode(String projectCode, FunctionNodeRequest request) {
        ProjectModule projectModule = requireProject(projectCode);
        FunctionNode functionNode = new FunctionNode(
                functionIdGenerator.getAndIncrement(),
                request.parentId(),
                request.code(),
                request.name(),
                request.nodeType(),
                projectModule.getFunctionNodes().size() * 10 + 10
        );
        functionNode.setRoutePath(request.routePath());
        functionNode.setExternalUrl(request.externalUrl());
        functionNode.setSsoEnabled(request.ssoEnabled());
        projectModule.getFunctionNodes().add(functionNode);
        return functionNode;
    }

    private void initProjects() {
        ProjectModule userAdmin = createSeedProject("user-admin", "用户管理", "用户、权限、项目模块和功能树配置", "UA", 10);
        userAdmin.getFunctionNodes().add(createSeedFunction(null, "users", "用户列表", "/admin/users", 10));
        userAdmin.getFunctionNodes().add(createSeedFunction(null, "projects", "项目配置", "/admin/projects", 20));
        userAdmin.getFunctionNodes().add(createSeedFunction(null, "function-tree", "功能树配置", "/admin/functions", 30));

        ProjectModule mihotel = createSeedProject("mihotel", "mihotel", "mihotel 运维项目模块", "MI", 20);
        mihotel.getFunctionNodes().add(createSeedFunction(null, "overview", "首页", "/projects/mihotel/overview", 10));
        mihotel.getFunctionNodes().add(createExternalSeedFunction(null, "external-tool", "外部工具入口", "https://example.com", 20));

        ProjectModule ihotel = createSeedProject("ihotel", "ihotel", "ihotel 运维项目模块", "IH", 30);
        ihotel.getFunctionNodes().add(createSeedFunction(null, "overview", "首页", "/projects/ihotel/overview", 10));
        ihotel.getFunctionNodes().add(createExternalSeedFunction(null, "sso-placeholder", "SSO 预留入口", "https://example.com/sso", 20));
    }

    private void initUsers() {
        UserAccount admin = new UserAccount(userIdGenerator.getAndIncrement(), "admin", "系统管理员", UserType.ADMIN, null);
        admin.getProjectCodes().addAll(projects.keySet());
        users.put(admin.getUsername(), admin);
    }

    private ProjectModule createSeedProject(String code, String name, String description, String iconText, int sortOrder) {
        ProjectModule projectModule = new ProjectModule(projectIdGenerator.getAndIncrement(), code, name, description, iconText, sortOrder);
        projects.put(projectModule.getCode(), projectModule);
        return projectModule;
    }

    private FunctionNode createSeedFunction(Long parentId, String code, String name, String routePath, int sortOrder) {
        FunctionNode functionNode = new FunctionNode(functionIdGenerator.getAndIncrement(), parentId, code, name,
                com.devpro.opsconsole.model.FunctionNodeType.MENU, sortOrder);
        functionNode.setRoutePath(routePath);
        return functionNode;
    }

    private FunctionNode createExternalSeedFunction(Long parentId, String code, String name, String externalUrl, int sortOrder) {
        FunctionNode functionNode = new FunctionNode(functionIdGenerator.getAndIncrement(), parentId, code, name,
                com.devpro.opsconsole.model.FunctionNodeType.EXTERNAL_LINK, sortOrder);
        functionNode.setExternalUrl(externalUrl);
        return functionNode;
    }

    private void grantProjects(UserAccount userAccount, List<String> projectCodes) {
        if (CollectionUtils.isEmpty(projectCodes)) {
            return;
        }
        projectCodes.stream()
                .filter(projects::containsKey)
                .forEach(userAccount.getProjectCodes()::add);
    }

    private UserAccount requireUser(String username) {
        UserAccount userAccount = users.get(username);
        if (userAccount == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return userAccount;
    }

    private ProjectModule requireProject(String projectCode) {
        ProjectModule projectModule = projects.get(projectCode);
        if (projectModule == null) {
            throw new IllegalArgumentException("项目模块不存在");
        }
        return projectModule;
    }

    private List<ProjectModule> sortedProjects(List<ProjectModule> projectModules) {
        return projectModules.stream()
                .sorted(Comparator.comparing(ProjectModule::getSortOrder))
                .toList();
    }
}

