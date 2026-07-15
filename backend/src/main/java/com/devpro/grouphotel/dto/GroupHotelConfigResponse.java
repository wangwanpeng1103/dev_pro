package com.devpro.grouphotel.dto;

import java.time.LocalDateTime;

/** 集团酒店配置列表响应，按维护需求返回可直接查看的连接配置。 */
public record GroupHotelConfigResponse(
        Long id, String hotelCode, String hotelName, String entityType, String addressConfig,
        String databaseUsername, String databaseHost, String databasePassword, Integer databasePort,
        String sshUsername, String sshHost, String sshPassword, Integer sshPort,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
