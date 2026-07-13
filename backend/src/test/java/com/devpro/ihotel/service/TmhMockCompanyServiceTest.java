package com.devpro.ihotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.devpro.ihotel.dto.TmhMockCompanyStatusRequest;
import com.devpro.ihotel.dto.TmhMockCompanyUpdateRequest;
import com.devpro.ihotel.entity.TmhMockCompanyEntity;
import com.devpro.ihotel.mapper.TmhMockCompanyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 天目湖模拟协议单位服务测试。
 */
class TmhMockCompanyServiceTest {

    @Test
    void shouldKeepEnterpriseCodeWhenUpdatingCompany() {
        TmhMockCompanyMapper mapper = mock(TmhMockCompanyMapper.class);
        TmhMockCompanyEntity entity = new TmhMockCompanyEntity();
        entity.setId(1L);
        entity.setEnterpriseCode("FIXED-CODE");
        entity.setEnterpriseName("原名称");
        entity.setOpenStatus(1);
        when(mapper.selectById(1L)).thenReturn(entity);
        TmhMockCompanyService service = new TmhMockCompanyService(mapper, new ObjectMapper());

        service.update(new TmhMockCompanyUpdateRequest(1L, "新名称", 0));

        ArgumentCaptor<TmhMockCompanyEntity> captor = ArgumentCaptor.forClass(TmhMockCompanyEntity.class);
        verify(mapper).updateById(captor.capture());
        assertEquals("FIXED-CODE", captor.getValue().getEnterpriseCode());
        assertEquals("新名称", captor.getValue().getEnterpriseName());
        assertEquals(0, captor.getValue().getOpenStatus());
        assertNotNull(captor.getValue().getUpdatedAt());
    }

    @Test
    void shouldRefreshUpdatedAtWhenChangingStatus() {
        TmhMockCompanyMapper mapper = mock(TmhMockCompanyMapper.class);
        TmhMockCompanyEntity entity = new TmhMockCompanyEntity();
        entity.setId(1L);
        entity.setEnterpriseCode("FIXED-CODE");
        entity.setEnterpriseName("测试单位");
        entity.setOpenStatus(1);
        when(mapper.selectById(1L)).thenReturn(entity);
        TmhMockCompanyService service = new TmhMockCompanyService(mapper, new ObjectMapper());

        service.changeStatus(new TmhMockCompanyStatusRequest(1L, 0));

        ArgumentCaptor<TmhMockCompanyEntity> captor = ArgumentCaptor.forClass(TmhMockCompanyEntity.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getOpenStatus());
        assertNotNull(captor.getValue().getUpdatedAt());
    }

    @Test
    void shouldDeleteExistingCompany() {
        TmhMockCompanyMapper mapper = mock(TmhMockCompanyMapper.class);
        TmhMockCompanyEntity entity = new TmhMockCompanyEntity();
        entity.setId(1L);
        when(mapper.selectById(1L)).thenReturn(entity);
        when(mapper.deleteById(1L)).thenReturn(1);
        TmhMockCompanyService service = new TmhMockCompanyService(mapper, new ObjectMapper());

        service.delete(1L);

        verify(mapper).deleteById(1L);
    }
    @Test
    void shouldBuildCompatiblePagedResponse() {
        TmhMockCompanyMapper mapper = mock(TmhMockCompanyMapper.class);
        when(mapper.selectPage(any(IPage.class), any())).thenAnswer(invocation -> {
            IPage<TmhMockCompanyEntity> page = invocation.getArgument(0);
            TmhMockCompanyEntity entity = new TmhMockCompanyEntity();
            entity.setId(1L);
            entity.setEnterpriseCode("KHCSZQL10FXS");
            entity.setEnterpriseName("测试中青旅10分销商");
            entity.setOpenStatus(1);
            page.setRecords(List.of(entity));
            page.setTotal(1L);
            return page;
        });
        TmhMockCompanyService service = new TmhMockCompanyService(mapper, new ObjectMapper());

        Map<String, Object> response = service.buildMockResponse(
                "member.queryPartnerPage",
                "{\"pageNo\":1,\"pageSize\":100}"
        );

        assertEquals("10000", response.get("code"));
        Map<?, ?> data = (Map<?, ?>) response.get("data");
        assertEquals(1L, data.get("total"));
        assertEquals(1, ((List<?>) data.get("corpResData")).size());
        assertEquals(1L, data.get("pageNo"));
        assertEquals(100L, data.get("pageSize"));
    }
}
