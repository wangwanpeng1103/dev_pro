package com.devpro.opsconsole.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 临时用户有效期延长请求，用于管理员快速给临时账号增加可用时间。
 *
 * @param extendHours 需要增加的小时数，必须为正整数
 */
public record TemporaryUserTimeExtendRequest(
        @NotNull @Positive Integer extendHours
) {
}
