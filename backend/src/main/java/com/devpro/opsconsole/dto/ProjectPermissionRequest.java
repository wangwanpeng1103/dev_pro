package com.devpro.opsconsole.dto;

import java.util.List;

/**
 * 用户项目授权请求。
 *
 * @param projectCodes 项目编码列表
 */
public record ProjectPermissionRequest(List<String> projectCodes) {
}

