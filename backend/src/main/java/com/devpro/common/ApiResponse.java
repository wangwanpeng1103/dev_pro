package com.devpro.common;

/**
 * 统一接口响应结构，便于前端按固定格式处理成功和失败结果。
 *
 * @param success 请求是否处理成功
 * @param message 响应提示信息
 * @param data 响应业务数据
 * @param <T> 响应数据类型
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    /**
     * 创建成功响应。
     *
     * @param data 响应业务数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "success", data);
    }

    /**
     * 创建失败响应。
     *
     * @param message 失败提示信息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
