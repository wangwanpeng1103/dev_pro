package com.devpro.opsconsole.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.devpro.opsconsole.entity.OpsFunctionNodeEntity;
import com.devpro.opsconsole.entity.OpsProjectEntity;
import com.devpro.opsconsole.entity.OpsUserProjectPermissionEntity;
import com.devpro.opsconsole.entity.SysUserEntity;
import com.devpro.opsconsole.mapper.OpsConsoleMapper;
import com.devpro.opsconsole.mapper.OpsFunctionNodeMapper;
import com.devpro.opsconsole.mapper.OpsProjectMapper;
import com.devpro.opsconsole.mapper.OpsUserProjectPermissionMapper;
import com.devpro.opsconsole.mapper.SysUserMapper;
import com.devpro.opsconsole.model.FunctionNode;
import com.devpro.opsconsole.model.FunctionNodeType;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.model.UserType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 运维控制台 MyBatis-Plus 数据访问层，负责用户、项目模块和项目功能入口的持久化读写。
 */
@Repository
public class OpsConsoleRepository {

    private static final int DEFAULT_SORT_STEP = 10;

    private final SysUserMapper sysUserMapper;
    private final OpsProjectMapper opsProjectMapper;
    private final OpsUserProjectPermissionMapper permissionMapper;
    private final OpsFunctionNodeMapper functionNodeMapper;
    private final OpsConsoleMapper opsConsoleMapper;

    public OpsConsoleRepository(
            SysUserMapper sysUserMapper,
            OpsProjectMapper opsProjectMapper,
            OpsUserProjectPermissionMapper permissionMapper,
            OpsFunctionNodeMapper functionNodeMapper,
            OpsConsoleMapper opsConsoleMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.opsProjectMapper = opsProjectMapper;
        this.permissionMapper = permissionMapper;
        this.functionNodeMapper = functionNodeMapper;
        this.opsConsoleMapper = opsConsoleMapper;
    }

    public Optional<UserAccount> findUserByUsername(String username) {
        SysUserEntity userEntity = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username));
        if (userEntity == null) {
            return Optional.empty();
        }
        UserAccount userAccount = toUserAccount(userEntity);
        loadProjectCodes(userAccount);
        return Optional.of(userAccount);
    }

    public Optional<String> findPasswordHashByUsername(String username) {
        SysUserEntity userEntity = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .select(SysUserEntity::getPasswordHash)
                .eq(SysUserEntity::getUsername, username));
        return Optional.ofNullable(userEntity).map(SysUserEntity::getPasswordHash);
    }

    public List<UserAccount> findAllUsers() {
        List<SysUserEntity> userEntities = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                .last("ORDER BY CASE WHEN username = 'admin' THEN 0 ELSE 1 END, id"));
        List<UserAccount> users = userEntities.stream()
                .map(this::toUserAccount)
                .toList();
        users.forEach(this::loadProjectCodes);
        return users;
    }

    /**
     * 按用户类型查询账号，并补齐每个用户已授权的项目编码。
     *
     * @param userType 用户类型
     * @return 指定类型的用户列表
     */
    public List<UserAccount> findUsersByType(UserType userType) {
        List<SysUserEntity> userEntities = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUserType, userType.name())
                .orderByAsc(SysUserEntity::getId));
        List<UserAccount> users = userEntities.stream()
                .map(this::toUserAccount)
                .toList();
        users.forEach(this::loadProjectCodes);
        return users;
    }

    /**
     * 新增用户主表记录，并同步写入项目授权关系。
     *
     * @param username 登录账号
     * @param displayName 展示名称
     * @param passwordHash 密码哈希
     * @param userType 用户类型
     * @param enabled 是否启用账号
     * @param expiresAt 临时用户过期时间，永久用户为空
     * @param projectCodes 授权项目编码集合
     * @return 新增后的用户信息
     */
    public UserAccount insertUser(String username, String displayName, String passwordHash, UserType userType,
            boolean enabled, LocalDateTime expiresAt, Collection<String> projectCodes) {
        SysUserEntity userEntity = new SysUserEntity();
        userEntity.setUsername(username);
        userEntity.setDisplayName(displayName);
        userEntity.setPasswordHash(passwordHash);
        userEntity.setUserType(userType.name());
        userEntity.setEnabled(enabled);
        userEntity.setExpiresAt(expiresAt);
        sysUserMapper.insert(userEntity);

        replaceUserProjects(username, projectCodes);
        UserAccount userAccount = toUserAccount(userEntity);
        userAccount.getProjectCodes().addAll(projectCodes);
        return userAccount;
    }

    /**
     * 修改永久用户基础信息，并重建该用户的项目授权关系。
     *
     * @param username 登录账号
     * @param displayName 展示名称
     * @param enabled 是否启用
     * @param projectCodes 授权项目编码集合
     */
    public void updatePermanentUser(String username, String displayName, boolean enabled, Collection<String> projectCodes) {
        sysUserMapper.update(new LambdaUpdateWrapper<SysUserEntity>()
                .set(SysUserEntity::getDisplayName, displayName)
                .set(SysUserEntity::getEnabled, enabled)
                .set(SysUserEntity::getExpiresAt, null)
                .eq(SysUserEntity::getUsername, username)
                .eq(SysUserEntity::getUserType, UserType.PERMANENT.name()));
        replaceUserProjects(username, projectCodes);
    }

    /**
     * 修改普通用户基础信息，并重建该用户的项目授权关系。
     *
     * @param username 登录账号
     * @param displayName 展示名称
     * @param enabled 是否启用
     * @param projectCodes 授权项目编码集合
     */
    public void updateUser(String username, String displayName, boolean enabled, Collection<String> projectCodes) {
        int updatedRows = sysUserMapper.update(new LambdaUpdateWrapper<SysUserEntity>()
                .set(SysUserEntity::getDisplayName, displayName)
                .set(SysUserEntity::getEnabled, enabled)
                .eq(SysUserEntity::getUsername, username)
                .ne(SysUserEntity::getUserType, UserType.ADMIN.name()));
        if (updatedRows == 0) {
            throw new IllegalArgumentException("用户不存在或不能修改管理员账号");
        }
        replaceUserProjects(username, projectCodes);
    }

    /**
     * 删除用户主表记录，并先清理项目授权关系，避免残留无效权限数据。
     *
     * @param username 登录账号
     */
    public void deleteUser(String username) {
        Long userId = findUserId(username);
        permissionMapper.delete(new LambdaQueryWrapper<OpsUserProjectPermissionEntity>()
                .eq(OpsUserProjectPermissionEntity::getUserId, userId));
        sysUserMapper.deleteById(userId);
    }

    /**
     * 修改指定用户的登录密码哈希。
     *
     * @param username 被修改密码的登录账号
     * @param passwordHash 新密码哈希
     */
    public void updateUserPassword(String username, String passwordHash) {
        int updatedRows = sysUserMapper.update(new LambdaUpdateWrapper<SysUserEntity>()
                .set(SysUserEntity::getPasswordHash, passwordHash)
                .eq(SysUserEntity::getUsername, username));
        if (updatedRows == 0) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    public void replaceUserProjects(String username, Collection<String> projectCodes) {
        Long userId = findUserId(username);
        permissionMapper.delete(new LambdaQueryWrapper<OpsUserProjectPermissionEntity>()
                .eq(OpsUserProjectPermissionEntity::getUserId, userId));
        for (String projectCode : projectCodes) {
            opsConsoleMapper.insertIgnoreUserProjectPermission(userId, projectCode);
        }
    }

    public List<ProjectModule> findAllProjects() {
        List<OpsProjectEntity> projectEntities = opsProjectMapper.selectList(new LambdaQueryWrapper<OpsProjectEntity>()
                .orderByAsc(OpsProjectEntity::getSortOrder)
                .orderByAsc(OpsProjectEntity::getId));
        List<ProjectModule> projects = projectEntities.stream()
                .map(this::toProjectModule)
                .toList();
        projects.forEach(project -> project.getFunctionNodes().addAll(findFunctionNodesByProjectCode(project.getCode())));
        return projects;
    }

    public List<ProjectModule> findProjectsForUser(UserAccount userAccount) {
        if (userAccount.getUserType() == UserType.ADMIN) {
            return findAllProjects();
        }
        List<ProjectModule> projects = opsConsoleMapper.selectEnabledProjectsByUserId(userAccount.getId()).stream()
                .map(this::toProjectModule)
                .toList();
        projects.forEach(project -> project.getFunctionNodes().addAll(findFunctionNodesByProjectCode(project.getCode())));
        return projects;
    }

    public ProjectModule insertProject(String code, String name, String description, String iconText) {
        Integer maxSortOrder = opsProjectMapper.selectList(new LambdaQueryWrapper<OpsProjectEntity>()
                        .select(OpsProjectEntity::getSortOrder)
                        .orderByDesc(OpsProjectEntity::getSortOrder)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(OpsProjectEntity::getSortOrder)
                .orElse(0);
        int sortOrder = maxSortOrder + DEFAULT_SORT_STEP;

        OpsProjectEntity projectEntity = new OpsProjectEntity();
        projectEntity.setProjectCode(code);
        projectEntity.setProjectName(name);
        projectEntity.setDescription(description);
        projectEntity.setIconText(iconText);
        projectEntity.setSortOrder(sortOrder);
        projectEntity.setEnabled(true);
        opsProjectMapper.insert(projectEntity);
        opsConsoleMapper.grantProjectToAdminUsers(code);
        return toProjectModule(projectEntity);
    }

    public Optional<ProjectModule> findProjectByCode(String projectCode) {
        OpsProjectEntity projectEntity = opsProjectMapper.selectOne(new LambdaQueryWrapper<OpsProjectEntity>()
                .eq(OpsProjectEntity::getProjectCode, projectCode));
        if (projectEntity == null) {
            return Optional.empty();
        }
        ProjectModule projectModule = toProjectModule(projectEntity);
        projectModule.getFunctionNodes().addAll(findFunctionNodesByProjectCode(projectCode));
        return Optional.of(projectModule);
    }

    public List<FunctionNode> findFunctionNodesByProjectCode(String projectCode) {
        Long projectId = findProjectId(projectCode);
        return functionNodeMapper.selectList(new LambdaQueryWrapper<OpsFunctionNodeEntity>()
                        .eq(OpsFunctionNodeEntity::getProjectId, projectId)
                        .eq(OpsFunctionNodeEntity::getEnabled, true)
                        .orderByAsc(OpsFunctionNodeEntity::getSortOrder)
                        .orderByAsc(OpsFunctionNodeEntity::getId))
                .stream()
                .map(this::toFunctionNode)
                .toList();
    }

    public FunctionNode insertFunctionNode(String projectCode, FunctionNode functionNode) {
        Long projectId = findProjectId(projectCode);
        Integer maxSortOrder = functionNodeMapper.selectList(new LambdaQueryWrapper<OpsFunctionNodeEntity>()
                        .select(OpsFunctionNodeEntity::getSortOrder)
                        .eq(OpsFunctionNodeEntity::getProjectId, projectId)
                        .orderByDesc(OpsFunctionNodeEntity::getSortOrder)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(OpsFunctionNodeEntity::getSortOrder)
                .orElse(0);
        int sortOrder = maxSortOrder + DEFAULT_SORT_STEP;

        OpsFunctionNodeEntity functionNodeEntity = new OpsFunctionNodeEntity();
        functionNodeEntity.setProjectId(projectId);
        functionNodeEntity.setParentId(functionNode.getParentId());
        functionNodeEntity.setNodeCode(functionNode.getCode());
        functionNodeEntity.setNodeName(functionNode.getName());
        functionNodeEntity.setNodeType(functionNode.getNodeType().name());
        functionNodeEntity.setRoutePath(functionNode.getRoutePath());
        functionNodeEntity.setExternalUrl(functionNode.getExternalUrl());
        functionNodeEntity.setSsoEnabled(functionNode.isSsoEnabled());
        functionNodeEntity.setSortOrder(sortOrder);
        functionNodeEntity.setEnabled(true);
        functionNodeMapper.insert(functionNodeEntity);
        return toFunctionNode(functionNodeEntity);
    }

    private void loadProjectCodes(UserAccount userAccount) {
        userAccount.getProjectCodes().addAll(opsConsoleMapper.selectProjectCodesByUserId(userAccount.getId()));
    }

    private Long findUserId(String username) {
        SysUserEntity userEntity = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .select(SysUserEntity::getId)
                .eq(SysUserEntity::getUsername, username));
        if (userEntity == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return userEntity.getId();
    }

    private Long findProjectId(String projectCode) {
        OpsProjectEntity projectEntity = opsProjectMapper.selectOne(new LambdaQueryWrapper<OpsProjectEntity>()
                .select(OpsProjectEntity::getId)
                .eq(OpsProjectEntity::getProjectCode, projectCode));
        if (projectEntity == null) {
            throw new IllegalArgumentException("项目模块不存在");
        }
        return projectEntity.getId();
    }

    private UserAccount toUserAccount(SysUserEntity userEntity) {
        UserAccount userAccount = new UserAccount(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getDisplayName(),
                UserType.valueOf(userEntity.getUserType()),
                userEntity.getExpiresAt()
        );
        userAccount.setEnabled(Boolean.TRUE.equals(userEntity.getEnabled()));
        return userAccount;
    }

    private ProjectModule toProjectModule(OpsProjectEntity projectEntity) {
        ProjectModule projectModule = new ProjectModule(
                projectEntity.getId(),
                projectEntity.getProjectCode(),
                projectEntity.getProjectName(),
                projectEntity.getDescription(),
                projectEntity.getIconText(),
                projectEntity.getSortOrder()
        );
        projectModule.setEnabled(Boolean.TRUE.equals(projectEntity.getEnabled()));
        return projectModule;
    }

    private FunctionNode toFunctionNode(OpsFunctionNodeEntity functionNodeEntity) {
        FunctionNode functionNode = new FunctionNode(
                functionNodeEntity.getId(),
                functionNodeEntity.getParentId(),
                functionNodeEntity.getNodeCode(),
                functionNodeEntity.getNodeName(),
                FunctionNodeType.valueOf(functionNodeEntity.getNodeType()),
                functionNodeEntity.getSortOrder()
        );
        functionNode.setRoutePath(functionNodeEntity.getRoutePath());
        functionNode.setExternalUrl(functionNodeEntity.getExternalUrl());
        functionNode.setSsoEnabled(Boolean.TRUE.equals(functionNodeEntity.getSsoEnabled()));
        functionNode.setEnabled(Boolean.TRUE.equals(functionNodeEntity.getEnabled()));
        return functionNode;
    }
}
