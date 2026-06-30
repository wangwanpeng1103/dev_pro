package com.devpro.opsconsole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户项目授权持久化实体，对应 ops_user_project_permission 表。
 */
@Getter
@Setter
@TableName("ops_user_project_permission")
public class OpsUserProjectPermissionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long projectId;
}
