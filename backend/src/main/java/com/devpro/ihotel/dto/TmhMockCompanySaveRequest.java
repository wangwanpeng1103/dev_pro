package com.devpro.ihotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 天目湖模拟协议单位新增请求。企业编码仅允许在新增时设置，并由数据库唯一索引保证唯一。
 */
public record TmhMockCompanySaveRequest(
        @NotBlank(message = "企业编码不能为空")
        @Size(max = 64, message = "企业编码不能超过64个字符")
        String enterpriseCode,
        @NotBlank(message = "企业名称不能为空")
        @Size(max = 200, message = "企业名称不能超过200个字符")
        String enterpriseName,
        Integer openStatus
) {
}
