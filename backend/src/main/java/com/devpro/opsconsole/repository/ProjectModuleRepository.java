package com.devpro.opsconsole.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devpro.opsconsole.entity.OpsProjectEntity;
import com.devpro.opsconsole.mapper.OpsProjectMapper;
import com.devpro.opsconsole.mapper.UserProjectPermissionQueryMapper;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.model.UserType;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 项目模块数据访问层，负责项目入口的查询、新增和基础转换。
 */
@Repository
public class ProjectModuleRepository {

    private final OpsProjectMapper opsProjectMapper;
    private final UserProjectPermissionQueryMapper permissionQueryMapper;

    public ProjectModuleRepository(
            OpsProjectMapper opsProjectMapper,
            UserProjectPermissionQueryMapper permissionQueryMapper
    ) {
        this.opsProjectMapper = opsProjectMapper;
        this.permissionQueryMapper = permissionQueryMapper;
    }

    public List<ProjectModule> findAllProjects() {
        return opsProjectMapper.selectList(new LambdaQueryWrapper<OpsProjectEntity>()
                        .orderByAsc(OpsProjectEntity::getSortOrder)
                        .orderByAsc(OpsProjectEntity::getId))
                .stream()
                .map(this::toProjectModule)
                .toList();
    }

    public List<ProjectModule> findProjectsForUser(UserAccount userAccount) {
        if (userAccount.getUserType() == UserType.ADMIN) {
            return findAllProjects();
        }
        return permissionQueryMapper.selectEnabledProjectsByUserId(userAccount.getId()).stream()
                .map(this::toProjectModule)
                .toList();
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
}
