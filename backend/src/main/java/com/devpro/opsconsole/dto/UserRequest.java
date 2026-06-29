package com.devpro.opsconsole.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * 用户创建请求，临时用户通过有效小时数控制过期时间。
 *
 * @param username 用户名
 * @param displayName 展示名称
 * @param validHours 临时用户有效小时数
 * @param projectCodes 授权项目编码列表
 */
public record UserRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        @Positive Integer validHours,
        List<String> projectCodes
) {
}

