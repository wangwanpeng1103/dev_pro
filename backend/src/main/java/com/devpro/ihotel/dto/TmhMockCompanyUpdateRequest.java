package com.devpro.ihotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 天目湖模拟协议单位修改请求。企业编码是不可变的唯一业务编号，不允许通过修改接口提交。
 */
public record TmhMockCompanyUpdateRequest(
        @NotNull(message = "协议单位主键不能为空")
        Long id,
        @NotBlank(message = "企业名称不能为空")
        @Size(max = 200, message = "企业名称不能超过200个字符")
        String enterpriseName,
        Integer openStatus
) {
}
