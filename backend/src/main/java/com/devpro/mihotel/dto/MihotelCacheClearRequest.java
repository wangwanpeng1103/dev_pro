package com.devpro.mihotel.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * mihotel 缓存清理请求，前端只传目标编码，真实地址由后端配置解析。
 *
 * @param targetCode 目标服务编码
 */
public record MihotelCacheClearRequest(@NotBlank String targetCode) {
}
