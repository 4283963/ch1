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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GoGatewayService {

    private static final Logger log = LoggerFactory.getLogger(GoGatewayService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${go.gateway.url}")
    private String goGatewayUrl;

    public boolean sendUpCommand(String spaceCode) {
        try {
            Map<String, String> bodyMap = new HashMap<>();
            bodyMap.put("spaceId", spaceCode);
            String url = goGatewayUrl + "/api/anchor/up";
            log.info("调用Go网关[升地锚], url={}, spaceCode={}", url, spaceCode);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(bodyMap, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("Go网关[升地锚]响应: {}", response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("调用Go网关[升地锚]失败, spaceCode={}", spaceCode, e);
            return false;
        }
    }

    public boolean sendDownCommand(String spaceCode) {
        try {
            Map<String, String> bodyMap = new HashMap<>();
            bodyMap.put("spaceId", spaceCode);
            String url = goGatewayUrl + "/api/anchor/down";
            log.info("调用Go网关[降地锚], url={}, spaceCode={}", url, spaceCode);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(bodyMap, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("Go网关[降地锚]响应: {}", response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("调用Go网关[降地锚]失败, spaceCode={}", spaceCode, e);
            return false;
        }
    }
}
