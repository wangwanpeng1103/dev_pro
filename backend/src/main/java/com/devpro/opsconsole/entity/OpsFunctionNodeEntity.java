package com.devpro.opsconsole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 项目功能节点持久化实体，对应 ops_function_node 表。
 */
@Getter
@Setter
@TableName("ops_function_node")
public class OpsFunctionNodeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long parentId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private String routePath;
    private String externalUrl;
    private Boolean ssoEnabled;
    private Integer sortOrder;
    private Boolean enabled;
}
