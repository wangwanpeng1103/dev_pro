package com.devpro.opsconsole.mapper;

import com.devpro.opsconsole.entity.OpsProjectEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 运维控制台复杂 SQL Mapper。多表关联和 INSERT IGNORE SELECT 统一放到 XML 中维护。
 */
@Mapper
public interface OpsConsoleMapper {

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

    /**
     * 给全部管理员账号授权指定项目。
     *
     * @param projectCode 项目编码
     * @return 写入行数
     */
    int grantProjectToAdminUsers(@Param("projectCode") String projectCode);
}
