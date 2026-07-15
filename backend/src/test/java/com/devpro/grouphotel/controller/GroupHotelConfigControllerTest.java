package com.devpro.grouphotel.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpro.common.PageResult;
import com.devpro.grouphotel.service.GroupHotelConfigService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 集团酒店配置查询接口测试。 */
class GroupHotelConfigControllerTest {

    /** 验证集团酒店代码为空时请求被校验层拒绝，不会进入保存服务。 */
    @Test
    void shouldRejectCreateWhenHotelCodeIsBlank() throws Exception {
        GroupHotelConfigService configService = mock(GroupHotelConfigService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new GroupHotelConfigController(configService))
                .build();

        mockMvc.perform(post("/api/group-hotel-management/configs/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hotelCode":" ","hotelName":"测试酒店","entityType":"HOTEL",
                                "addressConfig":"http://example.test","databaseUsername":"root",
                                "databaseHost":"127.0.0.1","databasePassword":"test","databasePort":3306}
                                """))
                .andExpect(status().isBadRequest());
    }
    /** 验证分页、类型和全模糊关键字参数能够传递给独立业务服务。 */
    @Test
    void shouldListGroupHotelConfigs() throws Exception {
        GroupHotelConfigService configService = mock(GroupHotelConfigService.class);
        when(configService.list(2, 20, "GROUP", "酒店")).thenReturn(new PageResult<>(List.of(), 0, 2, 20, 0));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new GroupHotelConfigController(configService))
                .build();

        mockMvc.perform(get("/api/group-hotel-management/configs")
                        .param("page", "2")
                        .param("pageSize", "20")
                        .param("entityType", "GROUP")
                        .param("keyword", "酒店"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.current").value(2));
        verify(configService).list(2, 20, "GROUP", "酒店");
    }

    /** 验证删除接口能够将记录主键传递给业务服务。 */
    @Test
    void shouldDeleteGroupHotelConfig() throws Exception {
        GroupHotelConfigService configService = mock(GroupHotelConfigService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new GroupHotelConfigController(configService))
                .build();

        mockMvc.perform(post("/api/group-hotel-management/configs/12/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(configService).delete(12L);
    }
}
