package com.smartpark.service;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AnnounceService {

    private static final Logger log = LoggerFactory.getLogger(AnnounceService.class);

    @Value("${speaker.enabled:true}")
    private boolean speakerEnabled;

    @Value("${speaker.base-url:http://localhost:8080}")
    private String speakerBaseUrl;

    @Value("${speaker.timeout-ms:3000}")
    private int timeoutMs;

    @Value("${speaker.template:请{ownerName}业主前往{area}{spaceCode}车位停车}")
    private String announceTemplate;

    @Autowired
    private RestTemplate restTemplate;

    public String buildAnnounceText(String ownerName, String area, String spaceCode) {
        String text = announceTemplate;
        text = text.replace("{ownerName}", ownerName != null ? ownerName : "尊敬的");
        text = text.replace("{area}", area != null ? area : "");
        text = text.replace("{spaceCode}", spaceCode != null ? spaceCode : "");
        return text;
    }

    @Async
    public void announceAsync(String speakerId, String text) {
        if (!speakerEnabled || speakerId == null || speakerId.isEmpty()) {
            log.info("[播报] 已跳过: enabled={}, speakerId={}", speakerEnabled, speakerId);
            return;
        }
        try {
            doAnnounce(speakerId, text);
        } catch (Exception e) {
            log.error("[播报] 异步调用失败, speakerId={}, text={}", speakerId, text, e);
        }
    }

    public boolean announceSync(String speakerId, String text) {
        if (!speakerEnabled || speakerId == null || speakerId.isEmpty()) {
            log.info("[播报] 已跳过: enabled={}, speakerId={}", speakerEnabled, speakerId);
            return true;
        }
        try {
            return doAnnounce(speakerId, text);
        } catch (Exception e) {
            log.error("[播报] 同步调用失败, speakerId={}, text={}", speakerId, text, e);
            return false;
        }
    }

    private boolean doAnnounce(String speakerId, String text) throws Exception {
        String url = speakerBaseUrl + "/api/announce/speak";
        log.info("[播报] 调用喇叭 speakerId={}, url={}, text={}", speakerId, url, text);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("speakerId", speakerId);
        bodyMap.put("text", text);
        bodyMap.put("volume", 80);
        bodyMap.put("priority", 1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(bodyMap, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        log.info("[播报] 响应 status={}, body={}", response.getStatusCode(), response.getBody());

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                Map<String, Object> respMap = JSON.parseObject(response.getBody(), Map.class);
                Object success = respMap.get("success");
                if (success != null && Boolean.FALSE.equals(success)) {
                    log.warn("[播报] 喇叭返回失败: {}", response.getBody());
                    return false;
                }
            } catch (Exception e) {
                log.debug("[播报] 非JSON响应，按成功处理: {}", response.getBody());
            }
            return true;
        }
        return false;
    }
}
