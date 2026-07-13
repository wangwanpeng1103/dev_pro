package com.devpro.ihotel.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 天目湖模拟协议单位启停请求。
 */
public record TmhMockCompanyStatusRequest(
        @NotNull(message = "协议单位主键不能为空") Long id,
        @NotNull(message = "启停状态不能为空") Integer openStatus
) {
}
