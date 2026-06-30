package com.devpro.opsconsole.controller;

import com.devpro.common.ApiResponse;
import com.devpro.opsconsole.model.ProjectModule;
import com.devpro.opsconsole.service.ProjectModuleService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目模块接口，负责项目入口列表和项目模块创建。
 */
@RestController
@RequestMapping("/api/project-modules")
public class ProjectModuleController {

    private final ProjectModuleService projectModuleService;

    public ProjectModuleController(ProjectModuleService projectModuleService) {
        this.projectModuleService = projectModuleService;
    }

    /**
     * 查询项目列表，传入用户名时仅返回该用户有权限的项目，否则返回全部项目。
     */
    @GetMapping
    public ApiResponse<List<ProjectModule>> listProjects(@RequestParam(required = false) String username) {
        if (username == null || username.isBlank()) {
            return ApiResponse.success(projectModuleService.listAllProjects());
        }
        return ApiResponse.success(projectModuleService.listProjectsForUser(username));
    }

}
