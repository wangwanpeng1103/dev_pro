package com.devpro.mihotel.dto;

/**
 * mihotel 系统参数查询环境，用于前端选择要访问的 mihotel 服务环境。
 *
 * @param code 环境编码
 * @param name 环境名称
 * @param sortOrder 排序号
 */
public record MihotelSystemParamEnvironmentResponse(String code, String name, int sortOrder) {
}
