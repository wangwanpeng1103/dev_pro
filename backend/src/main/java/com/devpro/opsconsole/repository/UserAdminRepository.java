package com.devpro.opsconsole.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devpro.common.PageResult;
import com.devpro.opsconsole.entity.OpsUserProjectPermissionEntity;
import com.devpro.opsconsole.entity.SysUserEntity;
import com.devpro.opsconsole.mapper.OpsUserProjectPermissionMapper;
import com.devpro.opsconsole.mapper.SysUserMapper;
import com.devpro.opsconsole.mapper.UserProjectPermissionQueryMapper;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.model.UserType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 用户管理数据访问层，负责用户主表和用户项目授权关系的读写边界。
 */
@Repository
public class UserAdminRepository {

    private final SysUserMapper sysUserMapper;
    private final OpsUserProjectPermissionMapper permissionMapper;
    private final UserProjectPermissionQueryMapper permissionQueryMapper;

    public UserAdminRepository(
            SysUserMapper sysUserMapper,
            OpsUserProjectPermissionMapper permissionMapper,
            UserProjectPermissionQueryMapper permissionQueryMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.permissionMapper = permissionMapper;
        this.permissionQueryMapper = permissionQueryMapper;
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

    /**
     * 分页查询用户列表，并保持 admin 账号固定排在第一页首位。
     *
     * @param page 当前页码，从 1 开始
     * @param pageSize 每页条数
     * @return 用户分页结果
     */
    public PageResult<UserAccount> findUsersPage(int page, int pageSize) {
        Page<SysUserEntity> entityPage = sysUserMapper.selectPage(
                Page.of(page, pageSize),
                new LambdaQueryWrapper<SysUserEntity>()
                        .last("ORDER BY CASE WHEN username = 'admin' THEN 0 ELSE 1 END, id")
        );
        List<UserAccount> users = entityPage.getRecords().stream()
                .map(this::toUserAccount)
                .toList();
        users.forEach(this::loadProjectCodes);
        return new PageResult<>(
                users,
                entityPage.getTotal(),
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getPages()
        );
    }

    /**
     * 新增用户主表记录，并同步写入项目授权关系。
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
     * 修改普通用户基础信息，并重建该用户的项目授权关系。
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
     */
    public void deleteUser(String username) {
        Long userId = findUserId(username);
        permissionMapper.delete(new LambdaQueryWrapper<OpsUserProjectPermissionEntity>()
                .eq(OpsUserProjectPermissionEntity::getUserId, userId));
        sysUserMapper.deleteById(userId);
    }

    /**
     * 修改指定用户的登录密码哈希。
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
            permissionQueryMapper.insertIgnoreUserProjectPermission(userId, projectCode);
        }
    }

    private void loadProjectCodes(UserAccount userAccount) {
        userAccount.getProjectCodes().addAll(permissionQueryMapper.selectProjectCodesByUserId(userAccount.getId()));
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
}
