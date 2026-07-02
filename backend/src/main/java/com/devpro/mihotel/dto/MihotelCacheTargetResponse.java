package com.devpro.mihotel.dto;

/**
 * mihotel 缓存清理目标，用于前端展示可操作的服务节点。
 *
 * @param code 目标编码
 * @param name 目标名称
 * @param environment 环境类型
 * @param sortOrder 展示顺序
 */
public record MihotelCacheTargetResponse(String code, String name, String environment, int sortOrder) {
}
