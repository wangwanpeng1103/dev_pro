package com.devpro.mihotel.controller;

import com.devpro.common.ApiResponse;
import com.devpro.mihotel.dto.MihotelSystemParamEnvironmentResponse;
import com.devpro.mihotel.dto.MihotelSystemParamQueryResponse;
import com.devpro.mihotel.dto.MihotelSystemParamSaveRequest;
import com.devpro.mihotel.service.MihotelSystemParamService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * mihotel 系统参数管理接口，负责向前端提供环境列表并代理查询 mihotel 原有系统参数接口。
 */
@RestController
@RequestMapping("/api/mihotel/system-params")
public class MihotelSystemParamController {

    private final MihotelSystemParamService systemParamService;

    public MihotelSystemParamController(MihotelSystemParamService systemParamService) {
        this.systemParamService = systemParamService;
    }

    /**
     * 查询系统参数可选环境。
     *
     * @return 可选环境列表
     */
    @GetMapping("/environments")
    public ApiResponse<List<MihotelSystemParamEnvironmentResponse>> listEnvironments() {
        return ApiResponse.success(systemParamService.listEnvironments());
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
            @RequestParam String hotelGroupCode
    ) {
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
        systemParamService.updateSystemParam(request);
        return ApiResponse.success(null);
    }
}
