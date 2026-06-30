package com.devpro.opsconsole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统用户持久化实体，对应 sys_user 表。
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String displayName;
    private String passwordHash;
    private String userType;
    private Boolean enabled;
    private LocalDateTime expiresAt;
}
