package com.devpro.mihotel.dto;

/**
 * mihotel 系统参数记录，字段保持与 mihotel 现有 OptionDO 返回结构兼容。
 *
 * @param id 系统参数主键
 * @param hotelGroupCode 集团代码
 * @param hotelCode 酒店代码
 * @param catalog 参数分类
 * @param item 参数项
 * @param setValue 设置值
 * @param defValue 默认值
 * @param isMod 是否允许修改
 * @param licCode 授权代码
 * @param descript 中文描述
 * @param descriptEn 英文描述
 * @param ctrlStr 控制串
 */
public record MihotelSystemParamRecordResponse(
        Long id,
        String hotelGroupCode,
        String hotelCode,
        String catalog,
        String item,
        String setValue,
        String defValue,
        String isMod,
        String licCode,
        String descript,
        String descriptEn,
        String ctrlStr
) {
}
