package com.devpro.cloudcheckin.service;

import com.devpro.cloudcheckin.dto.GroupAddressLookupResponse;
import com.devpro.cloudcheckin.dto.StoreCloudConfigLookupResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 云入住集团地址查询服务，按外部短链接口查询集团 ROP 注册地址。
 */
@Service
public class CloudCheckinGroupAddressService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String groupAddressQueryUrlTemplate;

    public CloudCheckinGroupAddressService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${cloud-checkin.group-address-query-url-template:"
                    + "http://api.ihotel.cn/s/url/{groupAppCode}}") String groupAddressQueryUrlTemplate
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.groupAddressQueryUrlTemplate = groupAddressQueryUrlTemplate;
    }

    /**
     * 根据集团代码查询集团地址；查询不到时返回 found=false，外部接口要求登录态时返回业务错误。
     *
     * @param groupCode 集团代码
     * @return 集团地址查询结果
     */
    public GroupAddressLookupResponse lookupGroupAddress(String groupCode) {
        String normalizedGroupCode = Optional.ofNullable(groupCode)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("集团代码不能为空"));
        String responseBody = requestExternalGroupAddress(normalizedGroupCode);
        return parseGroupAddress(normalizedGroupCode, responseBody);
    }

    /**
     * 根据门店代码查询云入住配置；外部接口返回 code=200 且提示没有 URL 记录时，视为未配置。
     *
     * @param storeCode 门店代码
     * @return 门店云入住配置查询结果
     */
    public StoreCloudConfigLookupResponse lookupStoreCloudConfig(String storeCode) {
        String normalizedStoreCode = Optional.ofNullable(storeCode)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("门店代码不能为空"));
        String responseBody = requestExternalStoreCloudConfig(normalizedStoreCode);
        return parseStoreCloudConfig(normalizedStoreCode, responseBody);
    }

    private String requestExternalGroupAddress(String groupCode) {
        String groupAppCode = groupCode + "_GROUP";
        return restClient.get()
                .uri(groupAddressQueryUrlTemplate, groupAppCode)
                .retrieve()
                .body(String.class);
    }

    private String requestExternalStoreCloudConfig(String storeCode) {
        String storeAppCode = storeCode + "_PMSIPAD";
        return restClient.get()
                .uri(groupAddressQueryUrlTemplate, storeAppCode)
                .retrieve()
                .body(String.class);
    }

    private GroupAddressLookupResponse parseGroupAddress(String groupCode, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new GroupAddressLookupResponse(false, groupCode, null, null);
        }
        String trimmedBody = responseBody.trim();
        try {
            JsonNode rootNode = objectMapper.readTree(trimmedBody);
            if (rootNode.path("code").asInt(-1) != 0) {
                return new GroupAddressLookupResponse(false, groupCode, null, null);
            }
            JsonNode dataNode = rootNode.path("data");
            if (!dataNode.isArray() || dataNode.isEmpty()) {
                return new GroupAddressLookupResponse(false, groupCode, null, null);
            }
            JsonNode firstRow = dataNode.get(0);
            String groupAddress = textValue(firstRow, "url");
            if (groupAddress == null || groupAddress.isBlank()) {
                return new GroupAddressLookupResponse(false, groupCode, textValue(firstRow, "name"), null);
            }
            return new GroupAddressLookupResponse(true, groupCode, textValue(firstRow, "name"), groupAddress);
        } catch (Exception exception) {
            throw new IllegalArgumentException("集团地址查询接口返回格式异常");
        }
    }

    private StoreCloudConfigLookupResponse parseStoreCloudConfig(String storeCode, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return emptyStoreCloudConfig(storeCode);
        }
        String trimmedBody = responseBody.trim();
        try {
            JsonNode rootNode = objectMapper.readTree(trimmedBody);
            if (rootNode.path("code").asInt(-1) != 0) {
                return emptyStoreCloudConfig(storeCode);
            }
            JsonNode dataNode = rootNode.path("data");
            if (!dataNode.isArray() || dataNode.isEmpty()) {
                return emptyStoreCloudConfig(storeCode);
            }
            JsonNode firstRow = dataNode.get(0);
            String rawConfig = textValue(firstRow, "url");
            if (rawConfig == null || rawConfig.isBlank()) {
                return emptyStoreCloudConfig(storeCode);
            }
            String[] configParts = rawConfig.split(";", -1);
            if (configParts.length < 6) {
                throw new IllegalArgumentException("门店云入住配置格式异常");
            }
            return new StoreCloudConfigLookupResponse(
                    true,
                    storeCode,
                    textValue(firstRow, "name"),
                    configParts[0].trim(),
                    configParts[1].trim(),
                    configParts[2].trim(),
                    configParts[3].trim(),
                    configParts[4].trim(),
                    configParts[5].trim(),
                    rawConfig
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("门店云入住配置查询接口返回格式异常");
        }
    }

    private StoreCloudConfigLookupResponse emptyStoreCloudConfig(String storeCode) {
        return new StoreCloudConfigLookupResponse(false, storeCode, null, null, null, null, null, null, null, null);
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        return value == null || value.isBlank() ? null : value;
    }
}
