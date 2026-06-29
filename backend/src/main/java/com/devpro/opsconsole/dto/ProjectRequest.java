package com.devpro.opsconsole.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 项目模块创建请求。
 *
 * @param code 项目编码
 * @param name 项目名称
 * @param description 项目描述
 * @param iconText 图标文本
 */
public record ProjectRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        String iconText
) {
}

