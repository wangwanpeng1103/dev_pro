package com.devpro.opsconsole.mapper;

import com.devpro.opsconsole.entity.OpsProjectEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户项目授权复杂 SQL Mapper，集中维护跨用户、项目、授权关系表的查询和写入。
 */
@Mapper
public interface UserProjectPermissionQueryMapper {

    /**
     * 查询用户已授权项目编码。
     *
     * @param userId 用户 ID
     * @return 项目编码列表
     */
    List<String> selectProjectCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询普通用户可访问的启用项目。
     *
     * @param userId 用户 ID
     * @return 项目列表
     */
    List<OpsProjectEntity> selectEnabledProjectsByUserId(@Param("userId") Long userId);

    /**
     * 按项目编码新增授权关系，重复授权自动忽略。
     *
     * @param userId 用户 ID
     * @param projectCode 项目编码
     * @return 写入行数
     */
    int insertIgnoreUserProjectPermission(@Param("userId") Long userId, @Param("projectCode") String projectCode);

}
