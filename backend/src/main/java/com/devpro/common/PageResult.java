package com.devpro.common;

import java.util.List;

/**
 * 统一分页响应结构，用于前端按页展示列表数据。
 *
 * @param records 当前页记录
 * @param total 总记录数
 * @param current 当前页码，从 1 开始
 * @param size 每页条数
 * @param pages 总页数
 * @param <T> 分页记录类型
 */
public record PageResult<T>(List<T> records, long total, long current, long size, long pages) {
}
