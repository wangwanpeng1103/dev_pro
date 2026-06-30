package com.devpro.opsconsole.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 项目内功能节点模型，当前用于菜单和入口配置。
 */
@Getter
@Setter
public class FunctionNode {

    private final Long id;
    private final Long parentId;
    private final String code;
    private String name;
    private FunctionNodeType nodeType;
    private String routePath;
    private String externalUrl;
    private boolean ssoEnabled;
    private int sortOrder;
    private boolean enabled;

    public FunctionNode(Long id, Long parentId, String code, String name, FunctionNodeType nodeType, int sortOrder) {
        this.id = id;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.nodeType = nodeType;
        this.sortOrder = sortOrder;
        this.enabled = true;
    }
}
