package com.devpro.mihotel.service;

import com.devpro.mihotel.dto.MihotelCacheClearResponse;
import com.devpro.mihotel.dto.MihotelCacheTargetResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * mihotel 缓存清理服务，按配置维护可清理的服务节点，并代理调用目标清缓存接口。
 */
@Service
public class MihotelClearCacheService {

    private static final String CLEAR_CACHE_PATH = "/mobilepmsapi/sys/clearAllCache";

    private final RestClient restClient;
    private final List<CacheTarget> targets;

    public MihotelClearCacheService(
            RestClient.Builder restClientBuilder,
            Environment environment
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds(environment)));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.targets = parseTargets(propertyValue(
                environment,
                "mihotel.clear-cache.trunk-targets",
                "MIHOTEL_CLEAR_CACHE_TRUNK_TARGETS"
        ), "TRUNK", 0);
        this.targets.addAll(parseTargets(propertyValue(
                environment,
                "mihotel.clear-cache.local-targets",
                "MIHOTEL_CLEAR_CACHE_LOCAL_TARGETS"
        ), "LOCAL", this.targets.size()));
    }

    /**
     * 查询当前已配置的缓存清理目标；未配置目标时返回空列表，由前端展示配置提示。
     *
     * @return 缓存清理目标列表
     */
    public List<MihotelCacheTargetResponse> listTargets() {
        return targets.stream()
                .sorted(Comparator.comparingInt(CacheTarget::sortOrder))
                .map(CacheTarget::toResponse)
                .toList();
    }

    /**
     * 根据目标编码调用 mihotel 原有清缓存接口，HTTP 2xx 即视为单节点清理成功。
     *
     * @param targetCode 目标服务编码
     * @return 单节点清理结果
     */
    public MihotelCacheClearResponse clearTarget(String targetCode) {
        CacheTarget target = findTarget(targetCode);
        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(URI.create(target.baseUrl() + CLEAR_CACHE_PATH))
                    .retrieve()
                    .toEntity(String.class);
            long durationMillis = System.currentTimeMillis() - startTime;
            int statusCode = response.getStatusCode().value();
            boolean success = response.getStatusCode().is2xxSuccessful();
            String message = success ? "清理成功" : "清理失败，目标服务返回异常状态";
            return target.toClearResponse(success, statusCode, durationMillis, message);
        } catch (RestClientException exception) {
            long durationMillis = System.currentTimeMillis() - startTime;
            return target.toClearResponse(false, 0, durationMillis, "清理失败：" + exception.getMessage());
        }
    }

    private CacheTarget findTarget(String targetCode) {
        String normalizedTargetCode = Optional.ofNullable(targetCode)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("缓存目标不能为空"));
        return targets.stream()
                .filter(target -> target.code().equals(normalizedTargetCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("缓存目标不存在"));
    }

    private List<CacheTarget> parseTargets(String configValue, String environment, int sortOffset) {
        if (configValue == null || configValue.isBlank()) {
            return new java.util.ArrayList<>();
        }
        String[] entries = configValue.split(",");
        List<CacheTarget> parsedTargets = new java.util.ArrayList<>();
        for (int index = 0; index < entries.length; index++) {
            String entry = entries[index].trim();
            if (entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split("\\|", -1);
            if (parts.length != 2 && parts.length != 3) {
                throw new IllegalArgumentException("mihotel 缓存清理目标配置格式异常");
            }
            String targetCode = parts[0].trim();
            String configuredName = parts.length == 3 ? parts[1].trim() : "";
            String baseUrl = parts.length == 3 ? parts[2].trim() : parts[1].trim();
            parsedTargets.add(new CacheTarget(
                    targetCode,
                    displayName(targetCode, configuredName, environment, baseUrl),
                    environment,
                    normalizeBaseUrl(baseUrl),
                    sortOffset + index
            ));
        }
        return parsedTargets;
    }

    private String displayName(String targetCode, String configuredName, String environment, String baseUrl) {
        String port = Optional.ofNullable(URI.create(baseUrl).getPort())
                .filter(value -> value > 0)
                .map(String::valueOf)
                .orElse("");
        if ("TRUNK".equals(environment) && targetCode.startsWith("trunk-")) {
            return port.isBlank() ? "主干服务" : "主干服务 " + port;
        }
        if ("LOCAL".equals(environment) && targetCode.startsWith("local-")) {
            return "本地环境";
        }
        return configuredName.isBlank() ? targetCode : configuredName;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("mihotel 缓存清理目标地址不能为空");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String propertyValue(Environment environment, String relaxedName, String envName) {
        String relaxedValue = environment.getProperty(relaxedName);
        if (relaxedValue != null && !relaxedValue.isBlank()) {
            return relaxedValue;
        }
        return Optional.ofNullable(environment.getProperty(envName)).orElse("");
    }

    private int readTimeoutSeconds(Environment environment) {
        String configuredValue = propertyValue(
                environment,
                "mihotel.clear-cache.read-timeout-seconds",
                "MIHOTEL_CLEAR_CACHE_READ_TIMEOUT_SECONDS"
        );
        if (configuredValue.isBlank()) {
            return 180;
        }
        try {
            return Integer.parseInt(configuredValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("mihotel 缓存清理超时时间配置必须是整数秒");
        }
    }

    private record CacheTarget(String code, String name, String environment, String baseUrl, int sortOrder) {

        private MihotelCacheTargetResponse toResponse() {
            return new MihotelCacheTargetResponse(code, name, environment, sortOrder);
        }

        private MihotelCacheClearResponse toClearResponse(
                boolean success,
                int httpStatus,
                long durationMillis,
                String message
        ) {
            return new MihotelCacheClearResponse(code, name, environment, success, httpStatus, durationMillis, message);
        }
    }
}
