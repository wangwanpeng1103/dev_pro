package com.devpro.ihotel.dto;

import java.time.LocalDateTime;

/**
 * 天目湖模拟协议单位管理列表响应。
 */
public record TmhMockCompanyResponse(
        Long id,
        String enterpriseCode,
        String enterpriseName,
        Integer openStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
