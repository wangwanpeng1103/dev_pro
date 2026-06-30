package com.devpro.opsconsole.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devpro.opsconsole.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 MyBatis-Plus Mapper，简单 CRUD 通过 BaseMapper 和 LambdaWrapper 完成。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {
}
