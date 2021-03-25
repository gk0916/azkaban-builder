package com.az.workflow.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


public class HttpUtil {
    private static RestTemplate restTemplate = new RestTemplate();
    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    public static String request(MultiValueMap params, String url) {
        try {
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            // 传递参数
            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, header);
            // 调用Azkaban的api
            return restTemplate.postForObject(url, httpEntity, String.class);
        } catch (RuntimeException e) {
            logger.error("", e);
            throw e;
        }
    }

    public static String request(HttpHeaders headers, MultiValueMap params, String url) {
        try {
            // 传递参数
            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);
            // 调用Azkaban的api
            return restTemplate.postForObject(url, httpEntity, String.class);
        } catch (RuntimeException e) {
            logger.error("", e);
            throw e;
        }
    }
}
