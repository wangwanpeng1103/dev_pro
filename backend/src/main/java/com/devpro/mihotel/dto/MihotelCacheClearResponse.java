package com.devpro.mihotel.dto;

/**
 * mihotel 单个服务缓存清理结果。
 *
 * @param code 目标编码
 * @param name 目标名称
 * @param environment 环境类型
 * @param success 是否清理成功
 * @param httpStatus 外部接口 HTTP 状态码
 * @param durationMillis 调用耗时，单位毫秒
 * @param message 结果提示
 */
public record MihotelCacheClearResponse(
        String code,
        String name,
        String environment,
        boolean success,
        int httpStatus,
        long durationMillis,
        String message
) {
}
