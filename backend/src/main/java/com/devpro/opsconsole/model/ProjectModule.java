package com.devpro.opsconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 运维项目模块模型，对应用户登录后的项目入口卡片。
 */
public class ProjectModule {

    private final Long id;
    private final String code;
    private String name;
    private String description;
    private String iconText;
    private int sortOrder;
    private boolean enabled;
    private final List<FunctionNode> functionNodes = new ArrayList<>();

    public ProjectModule(Long id, String code, String name, String description, String iconText, int sortOrder) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.iconText = iconText;
        this.sortOrder = sortOrder;
        this.enabled = true;
    }

    public Long getId() {
        return id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconText() {
        return iconText;
    }

    public void setIconText(String iconText) {
        this.iconText = iconText;
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

    public List<FunctionNode> getFunctionNodes() {
        return functionNodes;
    }
}

