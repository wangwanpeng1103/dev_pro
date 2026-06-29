package com.devpro.opsconsole.model;

/**
 * 项目内功能树节点模型，第一期不做节点级权限控制，只用于菜单和入口配置。
 */
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

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FunctionNodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(FunctionNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public boolean isSsoEnabled() {
        return ssoEnabled;
    }

    public void setSsoEnabled(boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

