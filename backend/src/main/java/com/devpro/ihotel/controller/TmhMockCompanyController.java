package com.devpro.ihotel.controller;

import com.devpro.common.ApiResponse;
import com.devpro.common.PageResult;
import com.devpro.ihotel.dto.TmhMockCompanyResponse;
import com.devpro.ihotel.dto.TmhMockCompanySaveRequest;
import com.devpro.ihotel.dto.TmhMockCompanyStatusRequest;
import com.devpro.ihotel.dto.TmhMockCompanyUpdateRequest;
import com.devpro.ihotel.service.TmhMockCompanyService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ihotel 天目湖接口模拟数据管理及对外拉取接口。
 */
@RestController
@RequestMapping("/api/ihotel/tmh-mock-companies")
public class TmhMockCompanyController {

    private final TmhMockCompanyService companyService;

    public TmhMockCompanyController(TmhMockCompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * 查询模拟协议单位管理列表。
     */
    @GetMapping
    public ApiResponse<PageResult<TmhMockCompanyResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(companyService.list(page, pageSize, keyword));
    }

    /**
     * 新增模拟协议单位。
     */
    @PostMapping("/create")
    public ApiResponse<TmhMockCompanyResponse> create(@Valid @RequestBody TmhMockCompanySaveRequest request) {
        return ApiResponse.success(companyService.create(request));
    }

    /**
     * 修改模拟协议单位名称和状态，企业编码创建后不可变。
     */
    @PostMapping("/update")
    public ApiResponse<TmhMockCompanyResponse> update(@Valid @RequestBody TmhMockCompanyUpdateRequest request) {
        return ApiResponse.success(companyService.update(request));
    }

    /**
     * 启用或停用模拟协议单位。
     */
    @PostMapping("/change-status")
    public ApiResponse<TmhMockCompanyResponse> changeStatus(
            @Valid @RequestBody TmhMockCompanyStatusRequest request
    ) {
        return ApiResponse.success(companyService.changeStatus(request));
    }

    /**
     * 删除模拟协议单位。
     */
    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ApiResponse.success(null);
    }

    /**
     * 供 PMS 直接配置并拉取的模拟入口。优先接收 PMS 签名请求中的 name 参数，
     * 同时保留 method 作为手工调试别名；签名字段仅为兼容调用保留，不在模拟环境校验。
     */
    @PostMapping(value = "/mock-api", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> mockApi(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String bizContent
    ) {
        String methodName = name != null && !name.isBlank() ? name : method;
        return companyService.buildMockResponse(methodName, bizContent);
    }
}
