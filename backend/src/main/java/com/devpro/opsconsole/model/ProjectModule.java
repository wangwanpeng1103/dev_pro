package com.devpro.opsconsole.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 运维项目模块模型，对应用户登录后的项目入口卡片。
 */
@Getter
public class ProjectModule {

    private final Long id;
    private final String code;
    @Setter
    private String name;
    @Setter
    private String description;
    @Setter
    private String iconText;
    @Setter
    private int sortOrder;
    @Setter
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
}
