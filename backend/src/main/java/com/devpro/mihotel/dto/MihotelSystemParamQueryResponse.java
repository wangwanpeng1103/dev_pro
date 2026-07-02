package com.devpro.mihotel.dto;

import java.util.List;

/**
 * mihotel 系统参数查询结果，保留查询条件和实际返回记录，方便前端展示当前上下文。
 *
 * @param environment 环境编码
 * @param environmentName 环境名称
 * @param hotelGroupCode 集团代码
 * @param records 系统参数记录
 */
public record MihotelSystemParamQueryResponse(
        String environment,
        String environmentName,
        String hotelGroupCode,
        List<MihotelSystemParamRecordResponse> records
) {
}
