package com.hnu.legal_cases.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 爬虫 HTTP 入口：列表搜索按国家代码映射到 crawl.json；案例详情见 detailUrl。
 */
@Data
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    /**
     * 案例详情 / case_details 使用的 crawl.json
     */
    private String detailUrl;

    /**
     * key 与 {@code CountryEnum} 的 code 一致（如 US、EU、JPN）；value 为列表搜索用 crawl.json 地址。
     */
    private Map<String, String> search = new HashMap<>();

    /**
     * 连接爬虫 HTTP 入口的超时（毫秒）。列表搜索与详情共用。
     */
    private int connectTimeoutMs = 10_000;

    /**
     * 列表搜索（单数据源一次 GET crawl.json）读超时。原全局 300s 过长，默认改为 120s，可按爬虫实际速度在 yml 里调大。
     */
    private int readTimeoutMsList = 120_000;

    /**
     * 案例详情抓取（正文可能很长）读超时，默认 5 分钟。
     */
    private int readTimeoutMsDetail = 300_000;

    /**
     * 详情 crawl.json 原始 HTTP 响应体上限（字节）。超过则放弃解析，避免小堆内存下 Jackson 反序列化 OOM。
     * 正文极长时可在爬虫侧截断，或提高 -Xmx / 调大本值。
     */
    private int maxDetailResponseBytes = 2_097_152;

    /**
     * 解析后单条正文字符数上限（utf-16 计），防止一条判词占满堆。
     */
    private int maxDetailItemChars = 350_000;

    /**
     * 列表搜索「整次请求」最长等待时间（毫秒）。多数据源并行时，到点即合并<strong>已完成</strong>的源，未完成的源不再等待。
     * ≤0 表示不限制（等同等到所有源结束或单源 HTTP 读超时），可能很慢。
     */
    private int searchTotalTimeoutMs = 90_000;
}
