package com.devpro.ihotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devpro.common.PageResult;
import com.devpro.ihotel.dto.TmhMockCompanyResponse;
import com.devpro.ihotel.dto.TmhMockCompanySaveRequest;
import com.devpro.ihotel.dto.TmhMockCompanyStatusRequest;
import com.devpro.ihotel.dto.TmhMockCompanyUpdateRequest;
import com.devpro.ihotel.dto.TmhMockPartnerResponse;
import com.devpro.ihotel.entity.TmhMockCompanyEntity;
import com.devpro.ihotel.mapper.TmhMockCompanyMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 天目湖接口模拟数据服务，负责管理数据并生成与真实接口兼容的分页报文。
 */
@Service
public class TmhMockCompanyService {

    private static final String PAGE_METHOD = "member.queryPartnerPage";
    private static final String UPDATE_METHOD = "member.queryUpdatePartnerInfo";
    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 1000;

    private final TmhMockCompanyMapper companyMapper;
    private final ObjectMapper objectMapper;

    public TmhMockCompanyService(TmhMockCompanyMapper companyMapper, ObjectMapper objectMapper) {
        this.companyMapper = companyMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询管理列表，可按编码或名称进行模糊搜索。
     */
    public PageResult<TmhMockCompanyResponse> list(int page, int pageSize, String keyword) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.min(Math.max(pageSize, 1), 100);
        String normalizedKeyword = Optional.ofNullable(keyword).map(String::trim).orElse("");
        LambdaQueryWrapper<TmhMockCompanyEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (!normalizedKeyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(TmhMockCompanyEntity::getEnterpriseCode, normalizedKeyword)
                    .or()
                    .like(TmhMockCompanyEntity::getEnterpriseName, normalizedKeyword));
        }
        // 管理列表固定按主键倒序，避免修改时间变化导致记录位置跳动而引发误操作。
        queryWrapper.orderByDesc(TmhMockCompanyEntity::getId);
        IPage<TmhMockCompanyEntity> result = companyMapper.selectPage(
                new Page<>(currentPage, currentPageSize), queryWrapper);
        return new PageResult<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getPages()
        );
    }

    /**
     * 新增模拟协议单位，企业编码作为同步业务主键，必须保持唯一。
     */
    @Transactional
    public TmhMockCompanyResponse create(TmhMockCompanySaveRequest request) {
        validateStatus(request.openStatus());
        TmhMockCompanyEntity entity = new TmhMockCompanyEntity();
        entity.setEnterpriseCode(request.enterpriseCode().trim());
        entity.setEnterpriseName(request.enterpriseName().trim());
        entity.setOpenStatus(Optional.ofNullable(request.openStatus()).orElse(1));
        try {
            companyMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("企业编码已存在");
        }
        return toResponse(companyMapper.selectById(entity.getId()));
    }

    /**
     * 修改模拟协议单位名称和状态。企业编码是唯一业务编号，创建后不可变；
     * 更新时间由服务显式刷新，增量接口可据此返回变更数据。
     */
    @Transactional
    public TmhMockCompanyResponse update(TmhMockCompanyUpdateRequest request) {
        validateStatus(request.openStatus());
        TmhMockCompanyEntity entity = requireCompany(request.id());
        // 修改接口不接收企业编码，避免唯一业务编号被客户端绕过页面篡改。
        entity.setEnterpriseName(request.enterpriseName().trim());
        entity.setOpenStatus(Optional.ofNullable(request.openStatus()).orElse(entity.getOpenStatus()));
        // 显式刷新更新时间，保证增量接口能按本次业务变更时间筛选到该记录。
        entity.setUpdatedAt(LocalDateTime.now());
        companyMapper.updateById(entity);
        return toResponse(companyMapper.selectById(entity.getId()));
    }

    /**
     * 单独切换启停状态，便于验证 PMS 对协议单位状态变化的同步处理。
     */
    @Transactional
    public TmhMockCompanyResponse changeStatus(TmhMockCompanyStatusRequest request) {
        validateStatus(request.openStatus());
        TmhMockCompanyEntity entity = requireCompany(request.id());
        entity.setOpenStatus(request.openStatus());
        // 启停属于需要同步给 PMS 的业务变更，必须同步推进增量游标时间。
        entity.setUpdatedAt(LocalDateTime.now());
        companyMapper.updateById(entity);
        return toResponse(companyMapper.selectById(entity.getId()));
    }

    /**
     * 删除模拟协议单位。删除后该单位不会再出现在全量或增量模拟接口中。
     */
    @Transactional
    public void delete(Long id) {
        requireCompany(id);
        if (companyMapper.deleteById(id) == 0) {
            throw new IllegalArgumentException("模拟协议单位删除失败");
        }
    }
    /**
     * 解析 PMS 表单中的 method 和 bizContent，并返回天目湖兼容报文。
     * 增量方法按 updated_at 时间窗口过滤，全量方法返回全部数据。
     */
    public Map<String, Object> buildMockResponse(String method, String bizContent) {
        validateMockMethod(method);
        JsonNode content = parseBizContent(bizContent);
        int pageNo = positiveInt(content.path("pageNo").asInt(1), 1);
        int pageSize = Math.min(positiveInt(content.path("pageSize").asInt(DEFAULT_PAGE_SIZE), DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);
        LambdaQueryWrapper<TmhMockCompanyEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (UPDATE_METHOD.equals(method)) {
            LocalDateTime startTime = parseRequestTime(content.path("startUpdateTime").asText(null), "开始时间");
            LocalDateTime endTime = parseRequestTime(content.path("endUpdateTime").asText(null), "结束时间");
            if (endTime.isBefore(startTime)) {
                throw new IllegalArgumentException("结束时间不能早于开始时间");
            }
            // 使用左闭右闭窗口，符合现有 PMS 游标推进方式；企业编码唯一可避免边界数据重复落库。
            queryWrapper.ge(TmhMockCompanyEntity::getUpdatedAt, startTime)
                    .le(TmhMockCompanyEntity::getUpdatedAt, endTime);
        }
        queryWrapper.orderByAsc(TmhMockCompanyEntity::getId);
        IPage<TmhMockCompanyEntity> result = companyMapper.selectPage(new Page<>(pageNo, pageSize), queryWrapper);
        List<TmhMockPartnerResponse> partners = result.getRecords().stream()
                .map(entity -> new TmhMockPartnerResponse(
                        entity.getEnterpriseCode(), entity.getOpenStatus(), entity.getEnterpriseName()))
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("corpResData", partners);
        data.put("pageNo", result.getCurrent());
        data.put("pageSize", result.getSize());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "10000");
        response.put("msg", "success");
        response.put("data", data);
        return response;
    }

    /**
     * 只接受 PMS 当前使用的全量和增量方法名，避免参数拼写错误时误返回全量数据。
     */
    private void validateMockMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("接口方法名不能为空");
        }
        if (!PAGE_METHOD.equals(method) && !UPDATE_METHOD.equals(method)) {
            throw new IllegalArgumentException("不支持的接口方法名");
        }
    }
    private JsonNode parseBizContent(String bizContent) {
        if (bizContent == null || bizContent.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(bizContent);
        } catch (Exception exception) {
            throw new IllegalArgumentException("bizContent 必须是合法 JSON");
        }
    }

    private LocalDateTime parseRequestTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return LocalDateTime.parse(value.trim(), REQUEST_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + "格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private int positiveInt(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private void validateStatus(Integer openStatus) {
        if (openStatus != null && openStatus != 0 && openStatus != 1) {
            throw new IllegalArgumentException("启停状态只能是0或1");
        }
    }

    private TmhMockCompanyEntity requireCompany(Long id) {
        TmhMockCompanyEntity entity = companyMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("模拟协议单位不存在");
        }
        return entity;
    }

    private TmhMockCompanyResponse toResponse(TmhMockCompanyEntity entity) {
        return new TmhMockCompanyResponse(
                entity.getId(), entity.getEnterpriseCode(), entity.getEnterpriseName(), entity.getOpenStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
