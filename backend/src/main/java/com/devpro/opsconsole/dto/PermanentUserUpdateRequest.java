package com.devpro.opsconsole.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 永久用户修改请求。
 *
 * @param displayName 展示名称
 * @param enabled 是否启用账号
 * @param projectCodes 授权项目编码列表
 */
public record PermanentUserUpdateRequest(
        @NotBlank String displayName,
        Boolean enabled,
        List<String> projectCodes
) {
}
