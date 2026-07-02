package com.devpro.mihotel.service;

import com.devpro.mihotel.dto.MihotelSystemParamEnvironmentResponse;
import com.devpro.mihotel.dto.MihotelSystemParamQueryResponse;
import com.devpro.mihotel.dto.MihotelSystemParamRecordResponse;
import com.devpro.mihotel.dto.MihotelSystemParamSaveRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * mihotel 系统参数查询代理服务，兼容 mihotel 现有 /operations/findSysOptionByCode 接口。
 */
@Service
public class MihotelSystemParamService {

    private static final String FIND_SYS_OPTION_PATH = "/mobilepmsapi/operations/findSysOptionByCode";
    private static final String ADD_OPTION_PATH = "/mobilepmsapi/operations/addOption";
    private static final String SAVE_OPTION_PATH = "/mobilepmsapi/operations/saveOption";
    private static final String TRUNK_ENVIRONMENT = "TRUNK";
    private static final String LOCAL_ENVIRONMENT = "LOCAL";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final List<SystemParamEnvironment> environments;

    public MihotelSystemParamService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            Environment environment
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds(environment)));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.environments = List.of(
                new SystemParamEnvironment(
                        TRUNK_ENVIRONMENT,
                        "主干环境",
                        normalizeBaseUrl(propertyValue(
                                environment,
                                "mihotel.system-param.trunk-base-url",
                                "MIHOTEL_SYSTEM_PARAM_TRUNK_BASE_URL",
                                "https://mobile.ihotel.cn"
                        )),
                        0
                ),
                new SystemParamEnvironment(
                        LOCAL_ENVIRONMENT,
                        "本地环境",
                        normalizeBaseUrl(propertyValue(
                                environment,
                                "mihotel.system-param.local-base-url",
                                "MIHOTEL_SYSTEM_PARAM_LOCAL_BASE_URL",
                                "http://192.168.23.112:8080"
                        )),
                        1
                )
        );
    }

    /**
     * 查询可用的系统参数环境，前端默认选择排序最靠前的主干环境。
     *
     * @return 系统参数查询环境列表
     */
    public List<MihotelSystemParamEnvironmentResponse> listEnvironments() {
        return environments.stream()
                .sorted(Comparator.comparingInt(SystemParamEnvironment::sortOrder))
                .map(SystemParamEnvironment::toResponse)
                .toList();
    }

    /**
     * 按集团代码和环境代理查询 mihotel 系统参数。
     *
     * @param environmentCode 环境编码
     * @param hotelGroupCode 集团代码
     * @return 系统参数查询结果
     */
    public MihotelSystemParamQueryResponse findSystemParams(String environmentCode, String hotelGroupCode) {
        String normalizedHotelGroupCode = Optional.ofNullable(hotelGroupCode)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("集团代码不能为空"));
        SystemParamEnvironment targetEnvironment = findEnvironment(environmentCode);
        URI queryUri = UriComponentsBuilder.fromUriString(targetEnvironment.baseUrl())
                .path(FIND_SYS_OPTION_PATH)
                .queryParam("hotelGroupCode", normalizedHotelGroupCode)
                .build(true)
                .toUri();
        String responseBody = requestMihotel(queryUri);
        List<MihotelSystemParamRecordResponse> records = parseRecords(responseBody);
        return new MihotelSystemParamQueryResponse(
                targetEnvironment.code(),
                targetEnvironment.name(),
                normalizedHotelGroupCode,
                records
        );
    }

    /**
     * 新增 mihotel 系统参数，按 mihotel 现有 addOption 接口要求提交 OptionDO 表单字段。
     *
     * @param request 系统参数新增请求
     */
    public void createSystemParam(MihotelSystemParamSaveRequest request) {
        validateCreateRequest(request);
        SystemParamEnvironment targetEnvironment = findEnvironment(request.environment());
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        addFormValue(formData, "hotelGroupCode", request.hotelGroupCode());
        addFormValue(formData, "catalog", request.catalog());
        addFormValue(formData, "item", request.item());
        addFormValue(formData, "setValue", request.setValue());
        addFormValue(formData, "defValue", request.defValue());
        addFormValue(formData, "descript", request.descript());
        addFormValue(formData, "descriptEn", request.descriptEn());
        addFormValue(formData, "ctrlStr", request.ctrlStr());
        requestMihotelForm(targetEnvironment, ADD_OPTION_PATH, formData);
    }

    /**
     * 修改 mihotel 系统参数。mihotel 现有 saveOption 只根据 id 更新 setValue，其它字段不在此处修改。
     *
     * @param request 系统参数修改请求
     */
    public void updateSystemParam(MihotelSystemParamSaveRequest request) {
        if (request == null || request.id() == null) {
            throw new IllegalArgumentException("系统参数主键不能为空");
        }
        String setValue = Optional.ofNullable(request.setValue()).orElse("");
        SystemParamEnvironment targetEnvironment = findEnvironment(request.environment());
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        addFormValue(formData, "id", String.valueOf(request.id()));
        addFormValue(formData, "setValue", setValue);
        requestMihotelForm(targetEnvironment, SAVE_OPTION_PATH, formData);
    }

    private String requestMihotel(URI queryUri) {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(queryUri)
                    .retrieve()
                    .toEntity(String.class);
            return Optional.ofNullable(response.getBody()).orElse("");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "mihotel 系统参数接口暂时不可用：" + exception.getMessage());
        }
    }

    private void requestMihotelForm(
            SystemParamEnvironment targetEnvironment,
            String path,
            MultiValueMap<String, String> formData
    ) {
        URI requestUri = UriComponentsBuilder.fromUriString(targetEnvironment.baseUrl())
                .path(path)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(requestUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toEntity(String.class);
            validateMihotelResponse(objectMapper.readTree(Optional.ofNullable(response.getBody()).orElse("{}")));
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "mihotel 系统参数保存接口暂时不可用：" + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, exception.getMessage());
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "mihotel 系统参数保存接口返回解析失败");
        }
    }

    private void validateCreateRequest(MihotelSystemParamSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("系统参数保存请求不能为空");
        }
        requiredValue(request.hotelGroupCode(), "集团代码不能为空");
        requiredValue(request.catalog(), "参数分类不能为空");
        requiredValue(request.item(), "参数项不能为空");
    }

    private void requiredValue(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void addFormValue(MultiValueMap<String, String> formData, String fieldName, String value) {
        if (value != null) {
            formData.add(fieldName, value);
        }
    }

    private List<MihotelSystemParamRecordResponse> parseRecords(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            validateMihotelResponse(rootNode);
            JsonNode dataNode = findDataNode(rootNode);
            if (dataNode == null || dataNode.isNull()) {
                return List.of();
            }
            JsonNode recordsNode = normalizeRecordsNode(dataNode);
            if (!recordsNode.isArray()) {
                throw new IllegalArgumentException("mihotel 系统参数接口返回格式异常");
            }
            return objectMapper.convertValue(
                    recordsNode,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class,
                            MihotelSystemParamRecordResponse.class
                    )
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, exception.getMessage());
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "mihotel 系统参数接口返回解析失败");
        }
    }

    private void validateMihotelResponse(JsonNode rootNode) {
        if (rootNode.has("success") && !rootNode.path("success").asBoolean(false)) {
            throw new IllegalArgumentException(mihotelMessage(rootNode, "mihotel 系统参数接口返回失败"));
        }
        JsonNode codeNode = rootNode.has("result") ? rootNode.path("result") : rootNode.path("code");
        if (!codeNode.isMissingNode() && !codeNode.isNull()) {
            String code = codeNode.asText();
            if (!"0".equals(code) && !"200".equals(code)) {
                throw new IllegalArgumentException(mihotelMessage(rootNode, "mihotel 系统参数接口返回失败"));
            }
        }
    }

    private JsonNode findDataNode(JsonNode rootNode) {
        for (String fieldName : List.of("retVal", "data", "rows", "list")) {
            JsonNode candidate = rootNode.path(fieldName);
            if (!candidate.isMissingNode()) {
                return candidate;
            }
        }
        return rootNode.isArray() ? rootNode : null;
    }

    private JsonNode normalizeRecordsNode(JsonNode dataNode) {
        if (dataNode.isArray()) {
            return dataNode;
        }
        for (String fieldName : List.of("retVal", "records", "rows", "list")) {
            JsonNode candidate = dataNode.path(fieldName);
            if (candidate.isArray()) {
                return candidate;
            }
        }
        return dataNode;
    }

    private String mihotelMessage(JsonNode rootNode, String fallbackMessage) {
        for (String fieldName : List.of("message", "msg", "error")) {
            String message = rootNode.path(fieldName).asText("");
            if (!message.isBlank()) {
                return message;
            }
        }
        return fallbackMessage;
    }

    private SystemParamEnvironment findEnvironment(String environmentCode) {
        String normalizedCode = Optional.ofNullable(environmentCode)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .orElse(TRUNK_ENVIRONMENT);
        return environments.stream()
                .filter(environment -> environment.code().equals(normalizedCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("mihotel 系统参数环境不存在"));
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("mihotel 系统参数环境地址不能为空");
        }
        String trimmedBaseUrl = baseUrl.trim();
        return trimmedBaseUrl.endsWith("/") ? trimmedBaseUrl.substring(0, trimmedBaseUrl.length() - 1) : trimmedBaseUrl;
    }

    private String propertyValue(Environment environment, String relaxedName, String envName, String defaultValue) {
        String relaxedValue = environment.getProperty(relaxedName);
        if (relaxedValue != null && !relaxedValue.isBlank()) {
            return relaxedValue;
        }
        return Optional.ofNullable(environment.getProperty(envName))
                .filter(value -> !value.isBlank())
                .orElse(defaultValue);
    }

    private int readTimeoutSeconds(Environment environment) {
        String configuredValue = propertyValue(
                environment,
                "mihotel.system-param.read-timeout-seconds",
                "MIHOTEL_SYSTEM_PARAM_READ_TIMEOUT_SECONDS",
                "60"
        );
        try {
            return Integer.parseInt(configuredValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("mihotel 系统参数查询超时时间配置必须是整数秒");
        }
    }

    private record SystemParamEnvironment(String code, String name, String baseUrl, int sortOrder) {

        private MihotelSystemParamEnvironmentResponse toResponse() {
            return new MihotelSystemParamEnvironmentResponse(code, name, sortOrder);
        }
    }
}
