package com.devpro.opsconsole.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数。
 *
 * @param username 用户名
 * @param password 密码
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

