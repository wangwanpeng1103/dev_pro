package com.devpro.ihotel.dto;

/**
 * 对外模拟接口中的协议单位数据，字段名称与天目湖真实接口保持一致。
 */
public record TmhMockPartnerResponse(
        String enterpriseCode,
        Integer openStatus,
        String enterpriseName
) {
}
