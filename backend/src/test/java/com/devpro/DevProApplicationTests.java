package com.devpro;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class DevProApplicationTests {

    /**
     * 验证应用启动入口存在，避免单元测试依赖外部 MySQL 环境。
     */
    @Test
    void applicationEntryPointExists() {
        assertDoesNotThrow(() -> Class.forName("com.devpro.DevProApplication"));
    }
}
