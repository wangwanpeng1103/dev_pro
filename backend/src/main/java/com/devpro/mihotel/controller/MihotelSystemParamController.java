package com.devpro.mihotel.controller;

import com.devpro.common.ApiResponse;
import com.devpro.mihotel.dto.MihotelSystemParamEnvironmentResponse;
import com.devpro.mihotel.dto.MihotelSystemParamQueryResponse;
import com.devpro.mihotel.dto.MihotelSystemParamSaveRequest;
import com.devpro.mihotel.service.MihotelSystemParamService;
import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.model.UserType;
import com.devpro.opsconsole.repository.UserAdminRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * mihotel 系统参数管理接口，负责向前端提供环境列表并代理查询 mihotel 原有系统参数接口。
 */
@RestController
@RequestMapping("/api/mihotel/system-params")
public class MihotelSystemParamController {

    private final MihotelSystemParamService systemParamService;
    private final UserAdminRepository userAdminRepository;

    public MihotelSystemParamController(
            MihotelSystemParamService systemParamService,
            UserAdminRepository userAdminRepository
    ) {
        this.systemParamService = systemParamService;
        this.userAdminRepository = userAdminRepository;
    }

    /**
     * 查询系统参数可选环境。
     *
     * @return 可选环境列表
     */
    @GetMapping("/environments")
    public ApiResponse<List<MihotelSystemParamEnvironmentResponse>> listEnvironments(
            @RequestParam(required = false) String operatorUsername
    ) {
        List<MihotelSystemParamEnvironmentResponse> environments = systemParamService.listEnvironments();
        if (isAdmin(operatorUsername)) {
            return ApiResponse.success(environments);
        }
        return ApiResponse.success(environments.stream()
                .filter(environment -> !"LOCAL".equals(environment.code()))
                .toList());
    }

    /**
     * 按环境和集团代码查询 mihotel 系统参数。
     *
     * @param environment 环境编码，默认主干
     * @param hotelGroupCode 集团代码
     * @return 系统参数查询结果
     */
    @GetMapping
    public ApiResponse<MihotelSystemParamQueryResponse> findSystemParams(
            @RequestParam(defaultValue = "TRUNK") String environment,
            @RequestParam String hotelGroupCode,
            @RequestParam(required = false) String operatorUsername
    ) {
        if (isLocalEnvironment(environment) && !isAdmin(operatorUsername)) {
            throw new ResponseStatusException(FORBIDDEN, "只有 admin 可以使用本地环境");
        }
        return ApiResponse.success(systemParamService.findSystemParams(environment, hotelGroupCode));
    }

    /**
     * 新增 mihotel 系统参数。
     *
     * @param request 系统参数保存请求
     * @return 空响应
     */
    @PostMapping("/create")
    public ApiResponse<Void> createSystemParam(@RequestBody MihotelSystemParamSaveRequest request) {
        if (!isAdmin(request.operatorUsername())) {
            throw new ResponseStatusException(FORBIDDEN, "只有 admin 可以新增系统参数");
        }
        systemParamService.createSystemParam(request);
        return ApiResponse.success(null);
    }

    /**
     * 修改 mihotel 系统参数。mihotel 现有保存接口只更新设置值。
     *
     * @param request 系统参数保存请求
     * @return 空响应
     */
    @PostMapping("/update")
    public ApiResponse<Void> updateSystemParam(@RequestBody MihotelSystemParamSaveRequest request) {
        ensureCanUpdate(request);
        systemParamService.updateSystemParam(request);
        return ApiResponse.success(null);
    }

    /**
     * 校验系统参数修改权限：管理员可修改全部参数，普通用户只允许维护 PMS SDK 接口参数。
     *
     * @param request 修改请求
     */
    private void ensureCanUpdate(MihotelSystemParamSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("系统参数保存请求不能为空");
        }
        if (isLocalEnvironment(request.environment()) && !isAdmin(request.operatorUsername())) {
            throw new ResponseStatusException(FORBIDDEN, "只有 admin 可以使用本地环境");
        }
        if (isAdmin(request.operatorUsername())) {
            return;
        }
        UserAccount operator = findOperator(request.operatorUsername());
        if (operator.getUserType() != UserType.ADMIN
                && "PMS".equals(request.catalog())
                && "INTERFACE_PARAMS".equals(request.item())) {
            return;
        }
        throw new ResponseStatusException(FORBIDDEN, "当前用户只能修改 PMS / INTERFACE_PARAMS 配置");
    }

    private boolean isAdmin(String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) {
            return false;
        }
        return userAdminRepository.findUserByUsername(operatorUsername.trim())
                .map(operator -> operator.getUserType() == UserType.ADMIN)
                .orElse(false);
    }

    private UserAccount findOperator(String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) {
            throw new ResponseStatusException(FORBIDDEN, "当前操作人不能为空");
        }
        return userAdminRepository.findUserByUsername(operatorUsername.trim())
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "当前操作人无效"));
    }

    private boolean isLocalEnvironment(String environment) {
        return "LOCAL".equalsIgnoreCase(String.valueOf(environment).trim().toUpperCase(Locale.ROOT));
    }
}
