package com.hnu.legal_cases.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseArgs;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoResVO;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.dto.crawler.CrawlerDetailArgs;
import com.hnu.legal_cases.dto.crawler.CrawlerDetailResVO;
import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.service.EuJpLegalSearchBridgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 兼容原 crawl.json 列表协议，供 {@code CrawlerClient} 将 EU/JPN 指向本机后端。
 */
@Slf4j
@RestController
@RequestMapping("/crawler-bridge")
@RequiredArgsConstructor
public class CrawlerBridgeController {

    private final ObjectMapper objectMapper;
    private final EuJpLegalSearchBridgeService bridge;

    @GetMapping(value = "/list/US/crawl.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public CrawlerBaseInfoResVO usList(
            @RequestParam("spider_name") String spiderName,
            @RequestParam(value = "start_requests", defaultValue = "true") String startRequests,
            @RequestParam("crawl_args") String crawlArgsJson) {
        return handle(CountryEnum.US, spiderName, crawlArgsJson);
    }

    @GetMapping(value = "/list/EU/crawl.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public CrawlerBaseInfoResVO euList(
            @RequestParam("spider_name") String spiderName,
            @RequestParam(value = "start_requests", defaultValue = "true") String startRequests,
            @RequestParam("crawl_args") String crawlArgsJson) {
        return handle(CountryEnum.EU, spiderName, crawlArgsJson);
    }

    @GetMapping(value = "/list/JPN/crawl.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public CrawlerBaseInfoResVO jpnList(
            @RequestParam("spider_name") String spiderName,
            @RequestParam(value = "start_requests", defaultValue = "true") String startRequests,
            @RequestParam("crawl_args") String crawlArgsJson) {
        return handle(CountryEnum.JPN, spiderName, crawlArgsJson);
    }

    private CrawlerBaseInfoResVO handle(CountryEnum region, String spiderName, String crawlArgsJson) {
        CrawlerBaseInfoResVO vo = new CrawlerBaseInfoResVO();
        String spider = spiderName == null ? "" : spiderName.trim();
        if (!region.getCode().equalsIgnoreCase(spider)) {
            log.warn("crawler-bridge spider_name mismatch: expected {} got {}", region.getCode(), spiderName);
            vo.setStatus("error");
            vo.setItems(List.of());
            return vo;
        }
        try {
            CrawlerBaseArgs args = objectMapper.readValue(crawlArgsJson, CrawlerBaseArgs.class);
            String kw = args.getKeyword();
            List<CrawlerBaseInfoItem> items = switch (region) {
                case US -> bridge.searchUs(kw, args.getYear());
                case EU -> bridge.searchEu(kw);
                case JPN -> bridge.searchJp(kw);
            };
            vo.setStatus("ok");
            vo.setItems(items);
            log.info("crawler-bridge {} keyword={} items={}", region.getCode(), kw, items.size());
        } catch (Exception e) {
            log.error("crawler-bridge {} failed", region.getCode(), e);
            vo.setStatus("error");
            vo.setItems(List.of());
        }
        return vo;
    }

    @GetMapping(value = "/detail/crawl.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public CrawlerDetailResVO detail(
            @RequestParam("spider_name") String spiderName,
            @RequestParam(value = "start_requests", defaultValue = "true") String startRequests,
            @RequestParam("crawl_args") String crawlArgsJson) {
        CrawlerDetailResVO vo = new CrawlerDetailResVO();
        String spider = spiderName == null ? "" : spiderName.trim();
        if (!"case_details".equalsIgnoreCase(spider)) {
            log.warn("crawler-bridge detail spider_name mismatch: {}", spiderName);
            vo.setStatus("error");
            vo.setItems(List.of());
            return vo;
        }
        try {
            CrawlerDetailArgs args = objectMapper.readValue(crawlArgsJson, CrawlerDetailArgs.class);
            String content = bridge.fetchDetail(args.getDetailUrl());
            vo.setStatus("ok");
            vo.setItems(content == null || content.isBlank() ? List.of() : List.of(content));
        } catch (Exception e) {
            log.error("crawler-bridge detail failed", e);
            vo.setStatus("error");
            vo.setItems(List.of());
        }
        return vo;
    }
}
