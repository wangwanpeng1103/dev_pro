package com.devpro.grouphotel.controller;

import com.devpro.common.ApiResponse;
import com.devpro.common.PageResult;
import com.devpro.grouphotel.dto.GroupHotelConfigCreateRequest;
import com.devpro.grouphotel.dto.GroupHotelConfigResponse;
import com.devpro.grouphotel.service.GroupHotelConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 集团酒店管理接口。 */
@RestController
@RequestMapping("/api/group-hotel-management/configs")
public class GroupHotelConfigController {
    private final GroupHotelConfigService configService;

    public GroupHotelConfigController(GroupHotelConfigService configService) {
        this.configService = configService;
    }

    /** 新增集团或酒店共享配置。 */
    @PostMapping("/create")
    public ApiResponse<GroupHotelConfigResponse> create(
            @Valid @RequestBody GroupHotelConfigCreateRequest request) {
        return ApiResponse.success(configService.create(request));
    }
    /** 修改集团或酒店共享配置。 */
    @PostMapping("/update")
    public ApiResponse<GroupHotelConfigResponse> update(
            @RequestParam Long id,
            @Valid @RequestBody GroupHotelConfigCreateRequest request) {
        return ApiResponse.success(configService.update(id, request));
    }

    /** 删除集团或酒店共享配置。 */
    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ApiResponse.success(null);
    }

    /** 查询集团酒店共享配置列表。 */
    @GetMapping
    public ApiResponse<PageResult<GroupHotelConfigResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(configService.list(page, pageSize, entityType, keyword));
    }
}
