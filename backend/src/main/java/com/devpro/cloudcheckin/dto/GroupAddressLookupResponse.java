package com.devpro.cloudcheckin.dto;

/**
 * 集团地址查询结果，用于云入住门店 ROP 信息注册页面自动带出集团地址。
 *
 * @param found 是否查询到集团数据
 * @param groupCode 查询使用的集团代码
 * @param groupName 集团名称
 * @param groupAddress 集团地址
 */
public record GroupAddressLookupResponse(
        boolean found,
        String groupCode,
        String groupName,
        String groupAddress
) {
}
