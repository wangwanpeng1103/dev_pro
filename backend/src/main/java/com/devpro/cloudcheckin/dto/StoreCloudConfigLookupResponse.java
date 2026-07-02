package com.devpro.cloudcheckin.dto;

/**
 * 门店云入住配置查询结果，用于地址校验页面展示并允许运维人员二次修正配置参数。
 *
 * @param found 是否查询到门店云入住配置
 * @param storeCode 查询使用的门店代码
 * @param configName 外部短链配置名称
 * @param groupAddress 集团服务地址
 * @param groupCode 集团代码，生成新配置时需要保留
 * @param username 云入住配置用户名
 * @param password 云入住配置密码
 * @param appKey 应用 Key
 * @param appSecret 应用密钥
 * @param rawConfig 外部接口返回的原始配置串
 */
public record StoreCloudConfigLookupResponse(
        boolean found,
        String storeCode,
        String configName,
        String groupAddress,
        String groupCode,
        String username,
        String password,
        String appKey,
        String appSecret,
        String rawConfig
) {
}
