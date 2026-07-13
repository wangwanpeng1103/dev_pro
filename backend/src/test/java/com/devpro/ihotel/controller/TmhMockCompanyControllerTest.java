package com.devpro.ihotel.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpro.ihotel.service.TmhMockCompanyService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 天目湖模拟接口 Controller 兼容性测试。
 */
class TmhMockCompanyControllerTest {

    /**
     * 验证接口可直接接收 PMS 签名工具生成的 name 和 bizContent 表单参数。
     */
    @Test
    void shouldAcceptPmsFormRequestWithNameParameter() throws Exception {
        TmhMockCompanyService companyService = mock(TmhMockCompanyService.class);
        String bizContent = "{\"startUpdateTime\":\"2026-07-13 00:00:00\","
                + "\"endUpdateTime\":\"2026-07-13 18:00:00\",\"pageNo\":1,\"pageSize\":100}";
        when(companyService.buildMockResponse("member.queryUpdatePartnerInfo", bizContent))
                .thenReturn(Map.of("code", "10000", "msg", "success", "data", Map.of()));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TmhMockCompanyController(companyService))
                .build();

        mockMvc.perform(post("/api/ihotel/tmh-mock-companies/mock-api")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appId", "test-app")
                        .param("name", "member.queryUpdatePartnerInfo")
                        .param("requestId", "test-request")
                        .param("timestamp", "1783936800000")
                        .param("version", "1.0")
                        .param("sign", "IGNORED")
                        .param("bizContent", bizContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("10000"));
        verify(companyService).buildMockResponse("member.queryUpdatePartnerInfo", bizContent);
    }
}
