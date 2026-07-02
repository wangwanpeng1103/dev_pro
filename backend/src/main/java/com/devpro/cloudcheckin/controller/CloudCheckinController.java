package com.devpro.cloudcheckin.controller;

import com.devpro.cloudcheckin.dto.GroupAddressLookupResponse;
import com.devpro.cloudcheckin.dto.StoreCloudConfigLookupResponse;
import com.devpro.cloudcheckin.service.CloudCheckinGroupAddressService;
import com.devpro.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 云入住项目接口，提供门店 ROP 信息注册相关辅助能力。
 */
@RestController
@RequestMapping("/api/cloud-checkin")
public class CloudCheckinController {

    private final CloudCheckinGroupAddressService groupAddressService;

    public CloudCheckinController(CloudCheckinGroupAddressService groupAddressService) {
        this.groupAddressService = groupAddressService;
    }

    /**
     * 按集团代码查询集团地址，用于前端自动带出并允许运维人员二次编辑。
     *
     * @param groupCode 集团代码
     * @return 集团地址查询结果
     */
    @GetMapping("/group-address")
    public ApiResponse<GroupAddressLookupResponse> lookupGroupAddress(@RequestParam String groupCode) {
        return ApiResponse.success(groupAddressService.lookupGroupAddress(groupCode));
    }

    /**
     * 按门店代码查询云入住配置，用于地址校验页面回显并生成修正后的配置串。
     *
     * @param storeCode 门店代码
     * @return 门店云入住配置查询结果
     */
    @GetMapping("/store-cloud-config")
    public ApiResponse<StoreCloudConfigLookupResponse> lookupStoreCloudConfig(@RequestParam String storeCode) {
        return ApiResponse.success(groupAddressService.lookupStoreCloudConfig(storeCode));
    }
}
