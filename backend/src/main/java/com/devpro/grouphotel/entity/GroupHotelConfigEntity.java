package com.devpro.grouphotel.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 集团酒店共享配置实体，同时承载地址服务、MySQL 和 SSH 连接信息。 */
@Getter
@Setter
@TableName("pms_connection_config")
public class GroupHotelConfigEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String hotelCode;
    private String hotelName;
    private String entityType;
    private String addressConfig;
    private String databaseUsername;
    private String databaseHost;
    private String databasePassword;
    private Integer databasePort;
    private String sshUsername;
    private String sshHost;
    private String sshPassword;
    /** SSH 未启用时必须显式写入 NULL，避免数据库历史默认值自动补成 22。 */
    @TableField(insertStrategy = FieldStrategy.ALWAYS)
    private Integer sshPort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
