package com.devpro.grouphotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 集团酒店共享配置新增请求。 */
public record GroupHotelConfigCreateRequest(
        @NotBlank(message = "集团酒店代码不能为空")
        @Size(max = 32, message = "集团酒店代码不能超过32个字符")
        String hotelCode,
        @Size(max = 200, message = "酒店名称不能超过200个字符")
        String hotelName,
        @Pattern(regexp = "^(GROUP|HOTEL)?$", message = "类型只能是GROUP或HOTEL")
        String entityType,
        @Size(max = 500, message = "地址服务配置不能超过500个字符")
        String addressConfig,
        @Size(max = 128, message = "数据库用户名不能超过128个字符")
        String databaseUsername,
        @Size(max = 255, message = "数据库地址不能超过255个字符")
        String databaseHost,
        @Size(max = 255, message = "数据库密码不能超过255个字符")
        String databasePassword,
        @Min(value = 1, message = "数据库端口必须在1到65535之间")
        @Max(value = 65535, message = "数据库端口必须在1到65535之间")
        Integer databasePort,
        @Size(max = 128, message = "SSH用户名不能超过128个字符")
        String sshUsername,
        @Size(max = 255, message = "SSH地址不能超过255个字符")
        String sshHost,
        @Size(max = 255, message = "SSH密码不能超过255个字符")
        String sshPassword,
        @Min(value = 1, message = "SSH端口必须在1到65535之间")
        @Max(value = 65535, message = "SSH端口必须在1到65535之间")
        Integer sshPort
) {
}
