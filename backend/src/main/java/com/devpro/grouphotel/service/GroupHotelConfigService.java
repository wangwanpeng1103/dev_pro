package com.devpro.grouphotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devpro.common.PageResult;
import com.devpro.grouphotel.dto.GroupHotelConfigCreateRequest;
import com.devpro.grouphotel.dto.GroupHotelConfigResponse;
import com.devpro.grouphotel.entity.GroupHotelConfigEntity;
import com.devpro.grouphotel.mapper.GroupHotelConfigMapper;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 集团酒店配置查询服务。 */
@Service
public class GroupHotelConfigService {
    private static final int MAX_PAGE_SIZE = 100;
    private final GroupHotelConfigMapper configMapper;

    public GroupHotelConfigService(GroupHotelConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    /**
     * 新增集团或酒店配置。集团酒店代码是共享业务主键，重复时不允许覆盖原配置。
     */
    @Transactional
    public GroupHotelConfigResponse create(GroupHotelConfigCreateRequest request) {
        GroupHotelConfigEntity entity = new GroupHotelConfigEntity();
        applyRequest(entity, request);
        try {
            configMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("集团酒店代码已存在");
        }
        return toResponse(configMapper.selectById(entity.getId()));
    }
    /** 根据主键修改配置，酒店代码仍由数据库唯一索引保证全局唯一。 */
    @Transactional
    public GroupHotelConfigResponse update(Long id, GroupHotelConfigCreateRequest request) {
        GroupHotelConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("集团酒店配置不存在");
        }
        applyRequest(entity, request);
        try {
            LambdaUpdateWrapper<GroupHotelConfigEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GroupHotelConfigEntity::getId, id)
                    .set(GroupHotelConfigEntity::getHotelCode, entity.getHotelCode())
                    .set(GroupHotelConfigEntity::getHotelName, entity.getHotelName())
                    .set(GroupHotelConfigEntity::getEntityType, entity.getEntityType())
                    .set(GroupHotelConfigEntity::getAddressConfig, entity.getAddressConfig())
                    .set(GroupHotelConfigEntity::getDatabaseUsername, entity.getDatabaseUsername())
                    .set(GroupHotelConfigEntity::getDatabaseHost, entity.getDatabaseHost())
                    .set(GroupHotelConfigEntity::getDatabasePassword, entity.getDatabasePassword())
                    .set(GroupHotelConfigEntity::getDatabasePort, entity.getDatabasePort())
                    .set(GroupHotelConfigEntity::getSshUsername, entity.getSshUsername())
                    .set(GroupHotelConfigEntity::getSshHost, entity.getSshHost())
                    .set(GroupHotelConfigEntity::getSshPassword, entity.getSshPassword())
                    .set(GroupHotelConfigEntity::getSshPort, entity.getSshPort());
            configMapper.update(null, updateWrapper);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("集团酒店代码已存在");
        }
        return toResponse(configMapper.selectById(id));
    }

    /** 删除指定集团酒店配置，不存在时返回明确业务错误。 */
    @Transactional
    public void delete(Long id) {
        if (configMapper.deleteById(id) == 0) {
            throw new IllegalArgumentException("集团酒店配置不存在");
        }
    }

    /** 将新增和修改请求统一映射到实体，保证两种保存方式的空值与大写规则一致。 */
    private void applyRequest(GroupHotelConfigEntity entity, GroupHotelConfigCreateRequest request) {
        entity.setHotelCode(request.hotelCode().trim().toUpperCase(Locale.ROOT));
        entity.setHotelName(trimToNull(request.hotelName()));
        entity.setEntityType(trimToNull(normalizeEntityType(request.entityType())));
        entity.setAddressConfig(trimToNull(request.addressConfig()));
        entity.setDatabaseUsername(trimToNull(request.databaseUsername()));
        entity.setDatabaseHost(trimToNull(request.databaseHost()));
        entity.setDatabasePassword(blankToNull(request.databasePassword()));
        entity.setDatabasePort(request.databasePort());
        entity.setSshUsername(trimToNull(request.sshUsername()));
        entity.setSshHost(trimToNull(request.sshHost()));
        entity.setSshPassword(blankToNull(request.sshPassword()));
        entity.setSshPort(request.sshPort());
    }
    /** 按类型精确筛选，并对集团酒店代码和名称执行全模糊分页查询。 */
    public PageResult<GroupHotelConfigResponse> list(int page, int pageSize, String entityType, String keyword) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String normalizedKeyword = Optional.ofNullable(keyword).map(String::trim).orElse("");
        String normalizedEntityType = normalizeEntityType(entityType);
        LambdaQueryWrapper<GroupHotelConfigEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (!normalizedEntityType.isBlank()) {
            queryWrapper.eq(GroupHotelConfigEntity::getEntityType, normalizedEntityType);
        }
        if (!normalizedKeyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like(GroupHotelConfigEntity::getHotelCode, normalizedKeyword)
                    .or().like(GroupHotelConfigEntity::getHotelName, normalizedKeyword));
        }
        queryWrapper.orderByDesc(GroupHotelConfigEntity::getId);
        IPage<GroupHotelConfigEntity> result = configMapper.selectPage(
                new Page<>(currentPage, currentPageSize), queryWrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    /** 密码字段只将纯空白值转为空，非空内容保持原样。 */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
    /** 可选连接字段统一清理首尾空格，空字符串按未配置处理。 */
    private String trimToNull(String value) {
        String normalized = Optional.ofNullable(value).map(String::trim).orElse("");
        return normalized.isBlank() ? null : normalized;
    }
    /** 类型只允许集团或酒店，空值表示查询全部。 */
    private String normalizeEntityType(String entityType) {
        String normalized = Optional.ofNullable(entityType).map(String::trim).orElse("").toUpperCase(Locale.ROOT);
        if (!normalized.isBlank() && !"GROUP".equals(normalized) && !"HOTEL".equals(normalized)) {
            throw new IllegalArgumentException("集团酒店类型只能是GROUP或HOTEL");
        }
        return normalized;
    }
    private GroupHotelConfigResponse toResponse(GroupHotelConfigEntity entity) {
        return new GroupHotelConfigResponse(entity.getId(), entity.getHotelCode(), entity.getHotelName(), entity.getEntityType(),
                entity.getAddressConfig(), entity.getDatabaseUsername(), entity.getDatabaseHost(),
                entity.getDatabasePassword(), entity.getDatabasePort(), entity.getSshUsername(), entity.getSshHost(),
                entity.getSshPassword(), entity.getSshPort(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
