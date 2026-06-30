package com.devpro.opsconsole.service;

import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.repository.ProjectModuleRepository;
import com.devpro.opsconsole.repository.UserAdminRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 项目模块业务服务，负责项目入口列表、新增项目以及项目可见范围。
 */
@Service
public class ProjectModuleService {

    private final ProjectModuleRepository projectModuleRepository;
    private final UserAdminRepository userAdminRepository;

    public ProjectModuleService(
            ProjectModuleRepository projectModuleRepository,
            UserAdminRepository userAdminRepository
    ) {
        this.projectModuleRepository = projectModuleRepository;
        this.userAdminRepository = userAdminRepository;
    }

    public List<ProjectModule> listProjectsForUser(String username) {
        UserAccount userAccount = userAdminRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return projectModuleRepository.findProjectsForUser(userAccount);
    }

    public List<ProjectModule> listAllProjects() {
        return projectModuleRepository.findAllProjects();
    }
}
