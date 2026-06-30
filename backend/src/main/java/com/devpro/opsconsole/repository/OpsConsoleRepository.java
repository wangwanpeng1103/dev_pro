package com.devpro.opsconsole.repository;

import com.devpro.opsconsole.model.FunctionNode;
import com.devpro.opsconsole.model.FunctionNodeType;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.model.UserType;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 运维控制台 MySQL 数据访问层，负责用户、项目模块和项目功能入口的持久化读写。
 */
@Repository
public class OpsConsoleRepository {

    private final JdbcTemplate jdbcTemplate;

    public OpsConsoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findUserByUsername(String username) {
        List<UserAccount> users = jdbcTemplate.query("""
                SELECT id, username, display_name, user_type, enabled, expires_at
                FROM sys_user
                WHERE username = ?
                """, userRowMapper(), username);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        UserAccount userAccount = users.getFirst();
        loadProjectCodes(userAccount);
        return Optional.of(userAccount);
    }

    public Optional<String> findPasswordHashByUsername(String username) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT password_hash FROM sys_user WHERE username = ?",
                    String.class,
                    username
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public List<UserAccount> findAllUsers() {
        List<UserAccount> users = jdbcTemplate.query("""
                SELECT id, username, display_name, user_type, enabled, expires_at
                FROM sys_user
                ORDER BY id
                """, userRowMapper());
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
        List<UserAccount> users = jdbcTemplate.query("""
                SELECT id, username, display_name, user_type, enabled, expires_at
                FROM sys_user
                WHERE user_type = ?
                ORDER BY id
                """, userRowMapper(), userType.name());
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
     * @param expiresAt 临时用户过期时间，永久用户为空
     * @param projectCodes 授权项目编码集合
     * @return 新增后的用户信息
     */
    public UserAccount insertUser(String username, String displayName, String passwordHash, UserType userType,
            LocalDateTime expiresAt, Collection<String> projectCodes) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO sys_user (username, display_name, password_hash, user_type, enabled, expires_at)
                    VALUES (?, ?, ?, ?, 1, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, username);
            statement.setString(2, displayName);
            statement.setString(3, passwordHash);
            statement.setString(4, userType.name());
            if (expiresAt == null) {
                statement.setTimestamp(5, null);
            } else {
                statement.setTimestamp(5, Timestamp.valueOf(expiresAt));
            }
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        UserAccount userAccount = new UserAccount(key == null ? null : key.longValue(), username, displayName, userType, expiresAt);
        replaceUserProjects(username, projectCodes);
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
        // 先更新用户主表，再重建项目授权，保证页面读取到的是最新账号状态。
        jdbcTemplate.update("""
                UPDATE sys_user
                SET display_name = ?, enabled = ?, expires_at = NULL
                WHERE username = ? AND user_type = 'PERMANENT'
                """, displayName, enabled, username);
        replaceUserProjects(username, projectCodes);
    }

    /**
     * 删除用户主表记录，并先清理项目授权关系，避免残留无效权限数据。
     *
     * @param username 登录账号
     */
    public void deleteUser(String username) {
        Long userId = findUserId(username);
        // 删除用户前先清理授权关系，避免留下孤立的项目权限数据。
        jdbcTemplate.update("DELETE FROM ops_user_project_permission WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
    }

    public void replaceUserProjects(String username, Collection<String> projectCodes) {
        Long userId = findUserId(username);
        jdbcTemplate.update("DELETE FROM ops_user_project_permission WHERE user_id = ?", userId);
        for (String projectCode : projectCodes) {
            jdbcTemplate.update("""
                    INSERT IGNORE INTO ops_user_project_permission (user_id, project_id)
                    SELECT ?, id FROM ops_project WHERE project_code = ?
                    """, userId, projectCode);
        }
    }

    public List<ProjectModule> findAllProjects() {
        List<ProjectModule> projects = jdbcTemplate.query("""
                SELECT id, project_code, project_name, description, icon_text, sort_order, enabled
                FROM ops_project
                ORDER BY sort_order, id
                """, projectRowMapper());
        projects.forEach(project -> project.getFunctionNodes().addAll(findFunctionNodesByProjectCode(project.getCode())));
        return projects;
    }

    public List<ProjectModule> findProjectsForUser(UserAccount userAccount) {
        if (userAccount.getUserType() == UserType.ADMIN) {
            return findAllProjects();
        }
        List<ProjectModule> projects = jdbcTemplate.query("""
                SELECT p.id, p.project_code, p.project_name, p.description, p.icon_text, p.sort_order, p.enabled
                FROM ops_project p
                JOIN ops_user_project_permission upp ON upp.project_id = p.id
                WHERE upp.user_id = ? AND p.enabled = 1
                ORDER BY p.sort_order, p.id
                """, projectRowMapper(), userAccount.getId());
        projects.forEach(project -> project.getFunctionNodes().addAll(findFunctionNodesByProjectCode(project.getCode())));
        return projects;
    }

    public ProjectModule insertProject(String code, String name, String description, String iconText) {
        Integer maxSortOrder = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(sort_order), 0) FROM ops_project", Integer.class);
        int sortOrder = maxSortOrder == null ? 10 : maxSortOrder + 10;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ops_project (project_code, project_name, description, icon_text, sort_order, enabled)
                    VALUES (?, ?, ?, ?, ?, 1)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setString(4, iconText);
            statement.setInt(5, sortOrder);
            return statement;
        }, keyHolder);
        grantProjectToAdminUsers(code);
        Number key = keyHolder.getKey();
        return new ProjectModule(key == null ? null : key.longValue(), code, name, description, iconText, sortOrder);
    }

    public Optional<ProjectModule> findProjectByCode(String projectCode) {
        List<ProjectModule> projects = jdbcTemplate.query("""
                SELECT id, project_code, project_name, description, icon_text, sort_order, enabled
                FROM ops_project
                WHERE project_code = ?
                """, projectRowMapper(), projectCode);
        if (projects.isEmpty()) {
            return Optional.empty();
        }
        ProjectModule projectModule = projects.getFirst();
        projectModule.getFunctionNodes().addAll(findFunctionNodesByProjectCode(projectCode));
        return Optional.of(projectModule);
    }

    public List<FunctionNode> findFunctionNodesByProjectCode(String projectCode) {
        return jdbcTemplate.query("""
                SELECT fn.id, fn.parent_id, fn.node_code, fn.node_name, fn.node_type, fn.route_path,
                       fn.external_url, fn.sso_enabled, fn.sort_order, fn.enabled
                FROM ops_function_node fn
                JOIN ops_project p ON p.id = fn.project_id
                WHERE p.project_code = ? AND fn.enabled = 1
                ORDER BY fn.sort_order, fn.id
                """, functionNodeRowMapper(), projectCode);
    }

    public FunctionNode insertFunctionNode(String projectCode, FunctionNode functionNode) {
        Long projectId = findProjectId(projectCode);
        Integer maxSortOrder = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(sort_order), 0)
                FROM ops_function_node
                WHERE project_id = ?
                """, Integer.class, projectId);
        int sortOrder = maxSortOrder == null ? 10 : maxSortOrder + 10;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ops_function_node
                    (project_id, parent_id, node_code, node_name, node_type, route_path, external_url,
                     sso_enabled, sort_order, enabled)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, projectId);
            if (functionNode.getParentId() == null) {
                statement.setObject(2, null);
            } else {
                statement.setLong(2, functionNode.getParentId());
            }
            statement.setString(3, functionNode.getCode());
            statement.setString(4, functionNode.getName());
            statement.setString(5, functionNode.getNodeType().name());
            statement.setString(6, functionNode.getRoutePath());
            statement.setString(7, functionNode.getExternalUrl());
            statement.setBoolean(8, functionNode.isSsoEnabled());
            statement.setInt(9, sortOrder);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        FunctionNode savedNode = new FunctionNode(key == null ? null : key.longValue(), functionNode.getParentId(),
                functionNode.getCode(), functionNode.getName(), functionNode.getNodeType(), sortOrder);
        savedNode.setRoutePath(functionNode.getRoutePath());
        savedNode.setExternalUrl(functionNode.getExternalUrl());
        savedNode.setSsoEnabled(functionNode.isSsoEnabled());
        return savedNode;
    }

    private void loadProjectCodes(UserAccount userAccount) {
        List<String> projectCodes = jdbcTemplate.queryForList("""
                SELECT p.project_code
                FROM ops_project p
                JOIN ops_user_project_permission upp ON upp.project_id = p.id
                WHERE upp.user_id = ?
                ORDER BY p.sort_order, p.id
                """, String.class, userAccount.getId());
        userAccount.getProjectCodes().addAll(projectCodes);
    }

    private void grantProjectToAdminUsers(String projectCode) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO ops_user_project_permission (user_id, project_id)
                SELECT u.id, p.id
                FROM sys_user u
                JOIN ops_project p ON p.project_code = ?
                WHERE u.user_type = 'ADMIN'
                """, projectCode);
    }

    private Long findUserId(String username) {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        if (userId == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return userId;
    }

    private Long findProjectId(String projectCode) {
        Long projectId = jdbcTemplate.queryForObject("SELECT id FROM ops_project WHERE project_code = ?", Long.class, projectCode);
        if (projectId == null) {
            throw new IllegalArgumentException("项目模块不存在");
        }
        return projectId;
    }

    private RowMapper<UserAccount> userRowMapper() {
        return (resultSet, rowNumber) -> {
            Timestamp expiresAt = resultSet.getTimestamp("expires_at");
            UserAccount userAccount = new UserAccount(
                    resultSet.getLong("id"),
                    resultSet.getString("username"),
                    resultSet.getString("display_name"),
                    UserType.valueOf(resultSet.getString("user_type")),
                    expiresAt == null ? null : expiresAt.toLocalDateTime()
            );
            userAccount.setEnabled(resultSet.getBoolean("enabled"));
            return userAccount;
        };
    }

    private RowMapper<ProjectModule> projectRowMapper() {
        return (resultSet, rowNumber) -> {
            ProjectModule projectModule = new ProjectModule(
                    resultSet.getLong("id"),
                    resultSet.getString("project_code"),
                    resultSet.getString("project_name"),
                    resultSet.getString("description"),
                    resultSet.getString("icon_text"),
                    resultSet.getInt("sort_order")
            );
            projectModule.setEnabled(resultSet.getBoolean("enabled"));
            return projectModule;
        };
    }

    private RowMapper<FunctionNode> functionNodeRowMapper() {
        return (resultSet, rowNumber) -> {
            Object parentId = resultSet.getObject("parent_id");
            FunctionNode functionNode = new FunctionNode(
                    resultSet.getLong("id"),
                    parentId == null ? null : ((Number) parentId).longValue(),
                    resultSet.getString("node_code"),
                    resultSet.getString("node_name"),
                    FunctionNodeType.valueOf(resultSet.getString("node_type")),
                    resultSet.getInt("sort_order")
            );
            functionNode.setRoutePath(resultSet.getString("route_path"));
            functionNode.setExternalUrl(resultSet.getString("external_url"));
            functionNode.setSsoEnabled(resultSet.getBoolean("sso_enabled"));
            functionNode.setEnabled(resultSet.getBoolean("enabled"));
            return functionNode;
        };
    }
}
