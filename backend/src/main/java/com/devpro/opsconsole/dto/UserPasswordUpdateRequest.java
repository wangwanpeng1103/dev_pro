package com.devpro.opsconsole.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理员修改用户密码请求。
 *
 * @param newPassword 新密码
 */
public record UserPasswordUpdateRequest(
        @NotBlank String newPassword
) {
}
