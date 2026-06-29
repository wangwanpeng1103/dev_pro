package com.devpro;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class DevProApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevProApplication.class);

    private final Environment environment;

    public DevProApplication(Environment environment) {
        this.environment = environment;
    }

    /**
     * 应用启动入口。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DevProApplication.class, args);
    }

    /**
     * 应用完全启动后输出醒目的成功提示，便于本地开发时快速确认服务可用。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationReady() {
        String port = environment.getProperty("server.port", "8080");
        String[] activeProfiles = environment.getActiveProfiles();
        String profileText = activeProfiles.length == 0
                ? Arrays.toString(environment.getDefaultProfiles())
                : Arrays.toString(activeProfiles);
        LOGGER.info("""

                ============================================================
                绿云运维控制台后端启动成功
                本地地址: http://localhost:{}
                健康检查: http://localhost:{}/api/health
                当前环境: {}
                ============================================================
                """, port, port, profileText);
    }
}

