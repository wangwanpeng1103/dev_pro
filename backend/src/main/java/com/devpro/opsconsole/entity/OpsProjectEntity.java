package com.devpro.opsconsole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 运维项目模块持久化实体，对应 ops_project 表。
 */
@Getter
@Setter
@TableName("ops_project")
public class OpsProjectEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectCode;
    private String projectName;
    private String description;
    private String iconText;
    private Integer sortOrder;
    private Boolean enabled;
}
