package com.devpro.opsconsole.dto;

import com.devpro.opsconsole.model.FunctionNodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 功能树节点创建请求。
 *
 * @param parentId 父节点ID
 * @param code 节点编码
 * @param name 节点名称
 * @param nodeType 节点类型
 * @param routePath 路由地址
 * @param externalUrl 外部链接
 * @param ssoEnabled 是否预留单点登录
 */
public record FunctionNodeRequest(
        Long parentId,
        @NotBlank String code,
        @NotBlank String name,
        @NotNull FunctionNodeType nodeType,
        String routePath,
        String externalUrl,
        boolean ssoEnabled
) {
}

