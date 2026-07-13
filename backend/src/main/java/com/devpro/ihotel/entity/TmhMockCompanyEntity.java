package com.devpro.ihotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 天目湖模拟协议单位实体，用于保存可由管理页面维护的接口测试数据。
 */
@Getter
@Setter
@TableName("ihotel_tmh_mock_company")
public class TmhMockCompanyEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String enterpriseCode;
    private String enterpriseName;
    private Integer openStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
