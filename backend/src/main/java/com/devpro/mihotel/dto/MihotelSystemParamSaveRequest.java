package com.devpro.mihotel.dto;

/**
 * mihotel 系统参数保存请求，新增时按 OptionDO 字段透传，修改时按 mihotel 现有逻辑只保存 setValue。
 *
 * @param environment 环境编码
 * @param id 系统参数主键，修改时必填
 * @param hotelGroupCode 集团代码
 * @param catalog 参数分类
 * @param item 参数项
 * @param setValue 设置值
 * @param defValue 默认值
 * @param descript 中文描述
 * @param descriptEn 英文描述
 * @param ctrlStr 控制串
 */
public record MihotelSystemParamSaveRequest(
        String environment,
        Long id,
        String hotelGroupCode,
        String catalog,
        String item,
        String setValue,
        String defValue,
        String descript,
        String descriptEn,
        String ctrlStr
) {
}
