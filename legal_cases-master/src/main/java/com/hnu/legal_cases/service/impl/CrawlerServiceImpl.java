package com.hnu.legal_cases.service.impl;

import com.alibaba.fastjson.JSON;
import com.hnu.legal_cases.client.CrawlerClient;
import com.hnu.legal_cases.config.CrawlerProperties;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItemNormalizer;
import com.hnu.legal_cases.dto.crawler.CrawlerSingleQueryResult;
import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.CrawlerService;
import com.hnu.legal_cases.service.SpringAIService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 获取案例基本信息并进行持久化
 *
 * @author baixu
 * @date 2025/8/24
 */
@Slf4j
@Service
public class CrawlerServiceImpl implements CrawlerService {

    @Autowired
    private CrawlerClient crawlerClient;
    @Autowired
    private SpringAIService springAIService;
    @Autowired
    @Qualifier("crawlerTaskExecutor")
    private Executor crawlerTaskExecutor;
    @Autowired
    private CrawlerProperties crawlerProperties;

    /**
     * 获取案例基本信息（支持多数据源并行查询）
     *
     * @param keyword 搜索关键词
     * @param country 国家代码（可为空，空则查询所有数据源）
     * @param period  时间段（可为空，空则不限制时间）
     * @return 案例基本信息列表
     */
    @Override
    public List<CrawlerBaseInfoItem> queryCaseBaseInfo(String keyword, String country, Integer period) {
        return queryCaseBaseInfo(keyword, country, period, null);
    }

    @Override
    public List<CrawlerBaseInfoItem> queryCaseBaseInfo(String keyword, String country, Integer period, String sourcesCsv) {
        List<CompletableFuture<CrawlerSingleQueryResult>> futures =
                startParallelCaseSearch(keyword, country, period, sourcesCsv, (a, b) -> { });
        return mergeDistinctAfterWait(futures);
    }

    @Override
    public List<CompletableFuture<CrawlerSingleQueryResult>> startParallelCaseSearch(
            String keyword,
            String country,
            Integer period,
            String sourcesCsv,
            BiConsumer<String, CrawlerSingleQueryResult> onSourceDone) {
        List<CountryEnum> targetCountries = determineTargetCountries(country, sourcesCsv);

        log.info("调用爬虫查询案例数据，搜索词: {}, 国家: {}, 时间段: {}, 数据源: {}",
                keyword, country, period, JSON.toJSONString(targetCountries));

        List<CompletableFuture<CrawlerSingleQueryResult>> futures = new ArrayList<>();
        for (CountryEnum countryEnum : targetCountries) {
            final String sourceCode = countryEnum.getCode();
            CompletableFuture<CrawlerSingleQueryResult> f = CompletableFuture.supplyAsync(() -> {
                        String searchKeyword = keyword;
                        if (targetCountries.size() > 1 && CountryEnum.JPN.equals(countryEnum)) {
                            searchKeyword = springAIService.translate(keyword, "English", "Japanese");
                        }
                        return crawlerClient.querySingleDataSource(searchKeyword, period, countryEnum);
                    },
                    crawlerTaskExecutor);
            f.whenComplete((res, ex) -> {
                CrawlerSingleQueryResult out = res;
                if (ex != null) {
                    log.error("数据源 {} 并行任务异常", sourceCode, ex);
                    out = CrawlerSingleQueryResult.failure();
                } else if (out == null) {
                    out = CrawlerSingleQueryResult.failure();
                }
                try {
                    onSourceDone.accept(sourceCode, out);
                } catch (Exception e) {
                    log.error("爬虫 onSourceDone 回调异常: {}", sourceCode, e);
                }
            });
            futures.add(f);
        }
        return futures;
    }

    @Override
    public List<CrawlerBaseInfoItem> mergeDistinctAfterWait(List<CompletableFuture<CrawlerSingleQueryResult>> futures) {
        waitForSearchFutures(futures);

        boolean anySourceRespondedOk = false;
        List<CrawlerBaseInfoItem> allResults = new ArrayList<>();
        for (CompletableFuture<CrawlerSingleQueryResult> future : futures) {
            if (!future.isDone()) {
                log.warn("列表搜索全局超时：本数据源尚未完成，已跳过");
                continue;
            }
            try {
                CrawlerSingleQueryResult result = future.get();
                if (result.isCrawlApiOk()) {
                    anySourceRespondedOk = true;
                }
                if (result.getItems() != null) {
                    allResults.addAll(result.getItems());
                }
            } catch (ExecutionException e) {
                log.error("查询数据源失败", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("合并爬虫结果被中断");
            }
        }

        for (CrawlerBaseInfoItem item : allResults) {
            CrawlerBaseInfoItemNormalizer.ensureStableDocketNumber(item);
        }

        List<CrawlerBaseInfoItem> distinctResults = allResults.stream()
                .filter(item -> StringUtils.isNotBlank(item.getTitle()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                CrawlerBaseInfoItem::getDocketNumber,
                                item -> item,
                                (existing, replacement) -> existing
                        ),
                        map -> new ArrayList<>(map.values())
                ));
        log.info("查询完成，共获取到 {} 条案例数据", distinctResults.size());
        if (distinctResults.isEmpty()) {
            if (anySourceRespondedOk) {
                log.info("爬虫接口至少一路返回 ok，但合并后无有效案例（无匹配或条目无标题）");
                return new ArrayList<>();
            }
            throw new ServiceException(
                    "调用爬虫搜索案例返回值为空。请确认各数据源爬虫服务已启动且网络可达，"
                            + "或在 application.yml 的 crawler.search 中配置正确地址（列表搜：EU:9002、US:9003、JPN:9004；详情 crawler.detail-url，默认 9001）。"
                            + "也可查看后端日志中「查询数据源 XX 失败」排查连接错误。");
        }
        return distinctResults;
    }

    /**
     * 查询案例详细信息
     *
     * @param url 案例url
     * @return 案例详细信息
     */
    @Override
    public String getCaseDetail(String url) {
        List<String> caseDetailList = crawlerClient.getCaseDetail(url);
        if (caseDetailList.size() != 1) {
            throw new ServiceException("查询案例详细信息结果数不为1，数量为：" + caseDetailList.size());
        }

        return caseDetailList.getFirst();
    }


    /**
     * 确定要查询的目标国家/数据源
     */
    private List<CountryEnum> determineTargetCountries(String country, String sourcesCsv) {
        if (StringUtils.isNotBlank(sourcesCsv)) {
            List<CountryEnum> list = new ArrayList<>();
            for (String part : sourcesCsv.split(",")) {
                if (part == null) {
                    continue;
                }
                String code = part.trim();
                if (code.isEmpty()) {
                    continue;
                }
                for (CountryEnum countryEnum : CountryEnum.values()) {
                    if (countryEnum.getCode().equals(code)) {
                        list.add(countryEnum);
                        break;
                    }
                }
            }
            if (list.isEmpty()) {
                throw new ServiceException("sources 未解析到有效数据源");
            }
            return list;
        }
        if (StringUtils.isBlank(country)) {
            return Arrays.asList(CountryEnum.values());
        }
        CountryEnum targetCountry = null;
        for (CountryEnum countryEnum : CountryEnum.values()) {
            if (countryEnum.getCode().equals(country)) {
                targetCountry = countryEnum;
                break;
            }
        }
        if (targetCountry == null) {
            throw new ServiceException("无效的国家");
        }
        return List.of(targetCountry);
    }

    /**
     * 控制列表搜索整体等待时间：避免「最慢一路」决定用户体感（多源并行时仍受 {@link CrawlerProperties#getReadTimeoutMsList()} 约束）。
     */
    @SuppressWarnings("unchecked")
    private void waitForSearchFutures(List<CompletableFuture<CrawlerSingleQueryResult>> futures) {
        if (futures.isEmpty()) {
            return;
        }
        CompletableFuture<CrawlerSingleQueryResult>[] arr = futures.toArray(new CompletableFuture[0]);
        CompletableFuture<Void> all = CompletableFuture.allOf(arr);
        int ms = futures.size() == 1
                ? Math.max(crawlerProperties.getReadTimeoutMsList() + 5_000, crawlerProperties.getSearchTotalTimeoutMs())
                : crawlerProperties.getSearchTotalTimeoutMs();
        if (ms <= 0) {
            all.join();
            return;
        }
        try {
            all.get(ms, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("列表搜索全局等待已达 {}ms，合并已完成数据源并返回（其余源仍在后台直至 HTTP 读超时）", ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待爬虫并行任务被中断");
        } catch (ExecutionException e) {
            log.warn("爬虫并行任务异常: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }
}