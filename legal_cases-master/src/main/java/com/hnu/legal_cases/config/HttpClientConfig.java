package com.hnu.legal_cases.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端：爬虫单独使用较短列表超时，避免一次搜索被 5 分钟读超时拖死。
 */
@Configuration
public class HttpClientConfig {

    /**
     * 列表搜索：多数据源并行，总耗时约为「最慢单源」，受 readTimeoutMsList 上限约束。
     */
    @Bean("crawlerListRestTemplate")
    public RestTemplate crawlerListRestTemplate(CrawlerProperties crawlerProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(crawlerProperties.getConnectTimeoutMs());
        factory.setReadTimeout(crawlerProperties.getReadTimeoutMsList());
        return new RestTemplate(factory);
    }

    /**
     * 案例详情：单页正文可能很长，允许更长读超时。
     */
    @Bean("crawlerDetailRestTemplate")
    public RestTemplate crawlerDetailRestTemplate(CrawlerProperties crawlerProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(crawlerProperties.getConnectTimeoutMs());
        factory.setReadTimeout(crawlerProperties.getReadTimeoutMsDetail());
        return new RestTemplate(factory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}