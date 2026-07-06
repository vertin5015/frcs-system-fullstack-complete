package com.hnu.legal_cases.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnu.legal_cases.config.CrawlerProperties;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseArgs;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoResVO;
import com.hnu.legal_cases.dto.crawler.CrawlerDetailArgs;
import com.hnu.legal_cases.dto.crawler.CrawlerDetailResVO;
import com.hnu.legal_cases.dto.crawler.CrawlerSingleQueryResult;
import com.hnu.legal_cases.enums.CountryEnum;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 调用爬虫接口获取案例基本信息（单个数据源）
 *
 * @author baixu
 * @date 2025/8/24
 */
@Slf4j
@Component
public class CrawlerClient {
    @Autowired
    @Qualifier("crawlerListRestTemplate")
    private RestTemplate crawlerListRestTemplate;
    @Autowired
    @Qualifier("crawlerDetailRestTemplate")
    private RestTemplate crawlerDetailRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CrawlerProperties crawlerProperties;

    private String searchBaseUrl(CountryEnum countryEnum) {
        String code = countryEnum.getCode();
        Map<String, String> m = crawlerProperties.getSearch();
        if (m == null) {
            log.error("crawler.search 未配置");
            return null;
        }
        String url = m.get(code);
        if (StringUtils.isBlank(url)) {
            url = m.get(code.toLowerCase());
        }
        if (StringUtils.isBlank(url)) {
            log.error("未配置 crawler.search.{}（或 {}）", code, code.toLowerCase());
        }
        return url;
    }

    private String detailUrl() {
        return crawlerProperties.getDetailUrl();
    }

    /**
     * 查询单个数据源的案例信息
     *
     * @param keyword     搜索关键词
     * @param period      时间段（可为空）
     * @param countryEnum 数据源枚举
     * @return 案例基本信息列表
     */
    public CrawlerSingleQueryResult querySingleDataSource(String keyword, Integer period, CountryEnum countryEnum) {
        try {
            // 构建crawl_args参数
            CrawlerBaseArgs crawlerArgs = new CrawlerBaseArgs();
            crawlerArgs.setKeyword(keyword);
            if (period != null) {
                crawlerArgs.setYear(String.valueOf(period));
            }
            String crawlArgsJson = objectMapper.writeValueAsString(crawlerArgs);

            String base = searchBaseUrl(countryEnum);
            if (StringUtils.isBlank(base)) {
                return CrawlerSingleQueryResult.failure();
            }

            // 构建请求URL
            String requestUrl = UriComponentsBuilder.fromUriString(base)
                    .queryParam("spider_name", countryEnum.getCode())
                    .queryParam("start_requests", "true")
                    .queryParam("crawl_args", crawlArgsJson)
                    .toUriString();

            // 发送HTTP请求
            ResponseEntity<CrawlerBaseInfoResVO> response = crawlerListRestTemplate.getForEntity(requestUrl, CrawlerBaseInfoResVO.class);
            if (response.getBody() != null && "ok".equals(response.getBody().getStatus())) {
                List<CrawlerBaseInfoItem> items = response.getBody().getItems();

                // 处理citationCount，提取数字
                if (items != null) {
                    items.forEach(item -> {
                        if (StringUtils.isNotBlank(item.getCitationCount())) {
                            String extractedNumber = this.extractNumberFromCitationCount(item.getCitationCount());
                            item.setCitationCount(extractedNumber);
                        }
                    });
                }

                log.info("数据源 {} 返回 {} 条数据", countryEnum.getCode(), items != null ? items.size() : 0);
                return CrawlerSingleQueryResult.success(items);
            }
            log.error("数据源 {} 返回状态异常: {}", countryEnum.getCode(), response.getBody());
            return CrawlerSingleQueryResult.failure();
        } catch (JsonProcessingException e) {
            log.error("数据源 {} JSON序列化失败", countryEnum.getCode(), e);
            return CrawlerSingleQueryResult.failure();
        } catch (Throwable e) {
            log.error("查询数据源 {} 失败", countryEnum.getCode(), e);
            return CrawlerSingleQueryResult.failure();
        }
    }

    /**
     * 根据案例url查询案例详细信息
     *
     * @param url 案例url
     * @return 案例详细信息列表（仅有一个值）
     */
    public List<String> getCaseDetail(String url) {
        try {
            String detailUrlNormalized = normalizeDetailUrlForFetch(url);
            CrawlerDetailArgs crawlerArgs = new CrawlerDetailArgs();
            crawlerArgs.setDetailUrl(detailUrlNormalized);
            String crawlArgsJson = objectMapper.writeValueAsString(crawlerArgs);

            // 构建请求URL
            String requestUrl = UriComponentsBuilder.fromUriString(detailUrl())
                    .queryParam("spider_name", "case_details")
                    .queryParam("start_requests", "true")
                    .queryParam("crawl_args", crawlArgsJson)
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            int maxBytes = Math.max(0, crawlerProperties.getMaxDetailResponseBytes());
            ResponseEntity<byte[]> raw = crawlerDetailRestTemplate.exchange(
                    URI.create(requestUrl), HttpMethod.GET, null, byte[].class);
            byte[] body = raw.getBody();
            if (body == null) {
                log.warn("根据url：{}（规范化后 {}）详情响应体为空", url, detailUrlNormalized);
                return new ArrayList<>();
            }
            if (maxBytes > 0 && body.length > maxBytes) {
                log.warn("根据url：{} 详情响应 {} bytes 超过上限 {}，已跳过解析以防 OOM",
                        url, body.length, maxBytes);
                return new ArrayList<>();
            }

            CrawlerDetailResVO parsed = objectMapper.readValue(body, CrawlerDetailResVO.class);
            if (parsed != null && "ok".equals(parsed.getStatus())) {
                List<String> items = capDetailItems(parsed.getItems());
                log.info("根据url：{} 查询案例详细信息返回 {} 条数据", url, items != null ? items.size() : 0);
                return items;
            }
            log.error("根据url：{} 查询案例详细信息返回状态异常: {}", url, parsed);
            return new ArrayList<>();

        } catch (JsonProcessingException e) {
            log.error("根据url：{} 查询案例详细信息JSON序列化失败", url, e);
            return new ArrayList<>();
        } catch (Throwable e) {
            log.error("根据url：{} 查询案例详细信息失败", url, e);
            return new ArrayList<>();
        }
    }

    /**
     * CourtListener 等站点把搜索参数挂在 opinion 链接上，易诱发详情爬虫返回整页搜索结果 JSON，导致小堆 OOM。
     */
    static String normalizeDetailUrlForFetch(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String u = raw.trim();
        if (u.contains("courtlistener.com") && u.contains("/opinion/")) {
            int q = u.indexOf('?');
            if (q > 0) {
                u = u.substring(0, q);
            }
            int h = u.indexOf('#');
            if (h > 0) {
                u = u.substring(0, h);
            }
        }
        return u;
    }

    private List<String> capDetailItems(List<String> items) {
        if (items == null) {
            return new ArrayList<>();
        }
        int cap = crawlerProperties.getMaxDetailItemChars();
        if (cap <= 0) {
            return items;
        }
        List<String> out = new ArrayList<>(items.size());
        for (String s : items) {
            if (s == null) {
                out.add(null);
                continue;
            }
            if (s.length() > cap) {
                out.add(s.substring(0, cap) + "\n\n[truncated: " + (s.length() - cap) + " chars omitted]");
            } else {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * 从citationCount中提取数字
     *
     * @param citationCount 原始字符串，如 "Cited by 0 opinions"
     * @return 提取的数字字符串，如 "0"
     */
    private String extractNumberFromCitationCount(String citationCount) {
        if (StringUtils.isBlank(citationCount)) {
            return "0";
        }

        // 使用正则表达式提取数字
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(citationCount);

        if (matcher.find()) {
            return matcher.group();
        }

        return "0"; // 如果没有找到数字，返回0
    }
}