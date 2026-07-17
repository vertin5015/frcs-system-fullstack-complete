package com.hnu.legal_cases.client;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerClientTest {

    @Test
    void buildCrawlerRequestUriEncodesJsonQueryParam() {
        String crawlArgsJson = "{\"details_url\":\"https://www.courts.go.jp/english/Judgments/search/2086/index.html\"}";

        URI uri = CrawlerClient.buildCrawlerRequestUri(
                "http://backend:8122/api/crawler-bridge/detail/crawl.json",
                "case_details",
                crawlArgsJson);

        assertThat(uri.toASCIIString()).contains("crawl_args=%7B%22details_url%22");
        assertThat(uri.toASCIIString()).doesNotContain("crawl_args={");
        assertThat(uri.getRawQuery()).doesNotContain("\"");
    }
}
