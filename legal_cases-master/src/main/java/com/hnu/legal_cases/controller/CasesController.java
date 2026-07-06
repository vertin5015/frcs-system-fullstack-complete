package com.hnu.legal_cases.controller;

import com.alibaba.fastjson.JSON;
import com.hnu.legal_cases.dto.ai.CaseQaReqVO;
import com.hnu.legal_cases.dto.ai.SummaryCaseReqVO;
import com.hnu.legal_cases.dto.ai.SummaryStatusResVO;
import com.hnu.legal_cases.dto.cases.CaseBaseInfo;
import com.hnu.legal_cases.dto.cases.SearchCasesReqVO;
import com.hnu.legal_cases.dto.cases.SearchCasesResVO;
import com.hnu.legal_cases.dto.cases.SearchStreamPartDTO;
import com.hnu.legal_cases.dto.favorite.FavoriteCaseResVO;
import com.hnu.legal_cases.dto.favorite.QueryFavoriteCaseReqVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.CaseDetailService;
import com.hnu.legal_cases.service.CaseService;
import com.hnu.legal_cases.service.CheckReqVOService;
import com.hnu.legal_cases.service.FavoriteService;
import com.hnu.legal_cases.service.OriginalDocumentCacheService;
import com.hnu.legal_cases.service.SearchCasesService;
import com.hnu.legal_cases.service.SearchStreamNotifier;
import com.hnu.legal_cases.service.SpringAIService;
import com.hnu.legal_cases.util.DistributedLock;
import com.hnu.legal_cases.util.JSONReturnBean;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/cases")
public class CasesController {
    @Autowired
    CheckReqVOService checkReqVOService;
    @Autowired
    SearchCasesService searchCasesService;
    @Autowired
    FavoriteService favoriteService;
    @Autowired
    DistributedLock distributedLock;
    @Autowired
    CaseService caseService;
    @Autowired
    CaseDetailService caseDetailService;
    @Autowired
    SpringAIService springAIService;
    @Autowired
    OriginalDocumentCacheService originalDocumentCacheService;

    @GetMapping(value = "/search", produces = "application/json")
    public JSONReturnBean<SearchCasesResVO> searchCases(@ModelAttribute SearchCasesReqVO reqVO) {
        String lockName = null;
        String lockValue = null;
        try {
            checkReqVOService.checkSearchCasesReqVO(reqVO);

            String country = reqVO.getCountry() != null ? reqVO.getCountry() : "ALL";
            String period = reqVO.getPeriod() != null ? reqVO.getPeriod().toString() : "ALL";
            String sources = reqVO.getSources() != null && !reqVO.getSources().isBlank() ? reqVO.getSources() : "ALL";
            lockName = distributedLock.getLockName("search", reqVO.getKeyword(), country, period, sources);

            lockValue = distributedLock.tryLock(lockName, 180);
            if (lockValue == null) {
                return JSONReturnBean.failed("系统繁忙，请稍后再试");
            }

            SearchCasesResVO resVO = searchCasesService.searchCases(reqVO);
            return JSONReturnBean.success(resVO);
        } catch (ServiceException e) {
            log.error("搜索案例异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("搜索案例错误，请稍后再试，error=", e);
            return JSONReturnBean.failed("搜索案例错误，请稍后再试");
        } finally {
            if (lockValue != null) {
                distributedLock.releaseLock(lockName, lockValue);
            }
        }
    }

    /**
     * Server-Sent Events：各数据源爬虫先返回时推送 {@code part}，落库结束后推送 {@code done}（结构与同步 {@link #searchCases} 一致）。
     */
    @GetMapping(value = "/search-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter searchCasesStream(@ModelAttribute SearchCasesReqVO reqVO) {
        SseEmitter emitter = new SseEmitter(0L);
        String lockName = null;
        String lockValue = null;
        try {
            checkReqVOService.checkSearchCasesReqVO(reqVO);

            String country = reqVO.getCountry() != null ? reqVO.getCountry() : "ALL";
            String period = reqVO.getPeriod() != null ? reqVO.getPeriod().toString() : "ALL";
            String sources = reqVO.getSources() != null && !reqVO.getSources().isBlank() ? reqVO.getSources() : "ALL";
            lockName = distributedLock.getLockName("search", reqVO.getKeyword(), country, period, sources);

            lockValue = distributedLock.tryLock(lockName, 180);
            if (lockValue == null) {
                try {
                    emitter.send(SseEmitter.event().name("fail")
                            .data(JSON.toJSONString(JSONReturnBean.failed("系统繁忙，请稍后再试"))));
                } catch (IOException ignored) {
                    log.warn("sse busy send");
                }
                emitter.complete();
                return emitter;
            }
        } catch (ServiceException e) {
            try {
                emitter.send(SseEmitter.event().name("fail").data(JSON.toJSONString(JSONReturnBean.failed(e.getMessage()))));
            } catch (IOException ignored) {
                log.warn("sse param send");
            }
            emitter.complete();
            return emitter;
        }

        final String fnLockName = lockName;
        final String fnLockValue = lockValue;
        final Object sendLock = new Object();

        SearchStreamNotifier notifier = new SearchStreamNotifier() {
            @Override
            public void part(SearchStreamPartDTO chunk) {
                synchronized (sendLock) {
                    try {
                        emitter.send(SseEmitter.event().name("part").data(JSON.toJSONString(chunk)));
                    } catch (IOException e) {
                        log.warn("sse part send", e);
                    }
                }
            }

            @Override
            public void done(SearchCasesResVO res) {
                synchronized (sendLock) {
                    try {
                        emitter.send(SseEmitter.event().name("done")
                                .data(JSON.toJSONString(JSONReturnBean.success(res))));
                    } catch (IOException e) {
                        log.warn("sse done send", e);
                    } finally {
                        distributedLock.releaseLock(fnLockName, fnLockValue);
                        emitter.complete();
                    }
                }
            }

            @Override
            public void fail(String message) {
                synchronized (sendLock) {
                    try {
                        emitter.send(SseEmitter.event().name("fail")
                                .data(JSON.toJSONString(JSONReturnBean.failed(message))));
                    } catch (IOException e) {
                        log.warn("sse fail send", e);
                    } finally {
                        distributedLock.releaseLock(fnLockName, fnLockValue);
                        emitter.complete();
                    }
                }
            }
        };

        CompletableFuture.runAsync(() -> {
            try {
                searchCasesService.searchCasesStream(reqVO, notifier);
            } catch (Throwable t) {
                log.error("search-stream", t);
                notifier.fail(t.getMessage() != null ? t.getMessage() : "搜索案例错误，请稍后再试");
            }
        });

        return emitter;
    }

    @GetMapping(value = "/summaryAsync/start", produces = "application/json")
    public JSONReturnBean<SummaryStatusResVO> startAsyncSummary(
            @ModelAttribute SummaryCaseReqVO reqVO,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        String lockName = null;
        String lockValue = null;
        try {
            checkReqVOService.checkSummaryCaseReqVO(reqVO);
            lockName = distributedLock.getLockName("summary", reqVO.getCaseId());
            lockValue = distributedLock.tryLock(lockName, 180);
            if (lockValue == null) {
                return JSONReturnBean.failed("系统繁忙，请稍后再试");
            }
            SummaryStatusResVO res = searchCasesService.startAsyncSummary(reqVO, force);
            return JSONReturnBean.success(res);
        } catch (ServiceException e) {
            log.error("异步摘要启动异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            if (e.getMessage() != null && e.getMessage().startsWith("QUOTA_EXCEEDED")) {
                return JSONReturnBean.failed(40301, e.getMessage());
            }
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("异步摘要启动错误，error=", e);
            return JSONReturnBean.failed("异步摘要启动失败，请稍后再试");
        } finally {
            if (lockValue != null) {
                distributedLock.releaseLock(lockName, lockValue);
            }
        }
    }

    @GetMapping(value = "/summaryAsync/status", produces = "application/json")
    public JSONReturnBean<SummaryStatusResVO> summaryAsyncStatus(@RequestParam String caseId, @RequestParam String language) {
        try {
            if (StringUtils.isBlank(caseId) || StringUtils.isBlank(language)) {
                return JSONReturnBean.failed("caseId 或 language 为空");
            }
            SummaryStatusResVO res = searchCasesService.getSummaryStatus(caseId, language);
            return JSONReturnBean.success(res);
        } catch (Throwable e) {
            log.error("查询摘要状态错误，error=", e);
            return JSONReturnBean.failed("查询摘要状态失败");
        }
    }

    @GetMapping(value = "/aisummary", produces = "application/json")
    public JSONReturnBean<String> summaryCase(@ModelAttribute SummaryCaseReqVO reqVO) {
        String lockName = null;
        String lockValue = null;
        try {
            checkReqVOService.checkSummaryCaseReqVO(reqVO);

            lockName = distributedLock.getLockName("summary", reqVO.getCaseId());
            lockValue = distributedLock.tryLock(lockName, 180);
            if (lockValue == null) {
                return JSONReturnBean.failed("系统繁忙，请稍后再试");
            }

            String content = searchCasesService.summaryCases(reqVO);
            return JSONReturnBean.success(content, "success");
        } catch (ServiceException e) {
            log.error("ai总结案例异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            if (e.getMessage() != null && e.getMessage().startsWith("QUOTA_EXCEEDED")) {
                return JSONReturnBean.failed(40301, e.getMessage());
            }
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("ai总结错误，请稍后再试，error=", e);
            return JSONReturnBean.failed("ai总结错误，请稍后再试");
        } finally {
            if (lockValue != null) {
                distributedLock.releaseLock(lockName, lockValue);
            }
        }
    }

    @GetMapping(value = "/meta", produces = "application/json")
    public JSONReturnBean<CaseBaseInfo> caseMeta(
            @RequestParam String caseId,
            @RequestParam String language,
            @RequestParam(required = false) Long userId) {
        try {
            if (StringUtils.isBlank(caseId) || StringUtils.isBlank(language)) {
                return JSONReturnBean.failed("caseId 或 language 为空");
            }
            Long uid = userId == null ? 0L : userId;
            List<CaseBaseInfo> list = caseService.getCasesByLanguage(Set.of(caseId), language, uid);
            if (list == null || list.isEmpty()) {
                return JSONReturnBean.failed("案例不存在");
            }
            return JSONReturnBean.success(list.get(0));
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("caseMeta", e);
            return JSONReturnBean.failed("查询案例失败");
        }
    }

    /**
     * 原文代理：将第三方页面通过后端抓取后返回给前端 iframe，避免目标站点 X-Frame-Options 限制。
     */
    @GetMapping(value = "/original-proxy")
    public ResponseEntity<byte[]> originalProxy(
            @RequestParam String url,
            @RequestParam(required = false, defaultValue = "false") boolean retry) {
        try {
            if (StringUtils.isBlank(url)) {
                return htmlError("缺少 url 参数");
            }
            String normalizedUrl = url.trim();
            if (retry) {
                originalDocumentCacheService.clearFailure(normalizedUrl);
            }
            String cached = originalDocumentCacheService.getCachedText(normalizedUrl);
            if (StringUtils.isNotBlank(cached)) {
                return readableHtml(readableHtmlBytes(cached));
            }
            URI uri = URI.create(normalizedUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return htmlError("仅支持 http/https");
            }
            String failure = originalDocumentCacheService.getLastFailure(normalizedUrl);
            if (StringUtils.isNotBlank(failure)) {
                return originalUnavailableHtml(normalizedUrl, failure);
            }
            // 立即返回窗口内占位页，后台抓取正文；避免左侧面板等待 20s 才有内容。
            originalDocumentCacheService.prefetch(normalizedUrl);
            return originalLoadingHtml(normalizedUrl);
        } catch (Exception e) {
            log.warn("original-proxy 失败: {}", e.getMessage());
            return htmlError("原文加载失败，请稍后重试");
        }
    }

    private ResponseEntity<byte[]> originalLoadingHtml(String url) {
        String safeUrl = url.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        String retryUrl = "/api/cases/original-proxy?retry=true&url=" + java.net.URLEncoder.encode(url, StandardCharsets.UTF_8);
        String html = "<!doctype html><html><meta charset='utf-8'/>"
                + "<meta http-equiv='refresh' content='3'>"
                + "<body style='font-family:Arial,Helvetica,sans-serif;line-height:1.7;padding:18px;color:#444'>"
                + "<h3 style='margin-top:0'>原文正在抓取</h3>"
                + "<p>原文站点响应较慢，系统已在后台继续抓取。此窗口会每 3 秒自动刷新，抓到后会直接显示正文。</p>"
                + "<p style='color:#888;font-size:13px'>来源：<a target='_blank' href=\"" + safeUrl + "\">外部打开原文</a> · <a href=\"" + retryUrl + "\">重新抓取</a></p>"
                + "</body></html>";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
                .header("Cache-Control", "no-store")
                .body(html.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseEntity<byte[]> originalUnavailableHtml(String url, String reason) {
        String safeUrl = url.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        String safeReason = reason == null ? "" : reason.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String retryUrl = "/api/cases/original-proxy?retry=true&url=" + java.net.URLEncoder.encode(url, StandardCharsets.UTF_8);
        String html = "<!doctype html><html><meta charset='utf-8'/>"
                + "<body style='font-family:Arial,Helvetica,sans-serif;line-height:1.7;padding:18px;color:#444'>"
                + "<h3 style='margin-top:0'>原文暂时无法在窗口内显示</h3>"
                + "<p>系统已经尝试通过后端抓取正文，但目标站点当前没有返回可读内容。你可以稍后重试，或先外部打开原文。</p>"
                + "<p style='color:#888;font-size:13px'>原因：" + safeReason + "</p>"
                + "<p><a style='display:inline-block;padding:8px 12px;background:#409eff;color:white;text-decoration:none;border-radius:4px' href=\"" + retryUrl + "\">重新抓取</a>"
                + "<a style='display:inline-block;margin-left:8px;padding:8px 12px;border:1px solid #409eff;color:#409eff;text-decoration:none;border-radius:4px' target='_blank' href=\"" + safeUrl + "\">外部打开原文</a></p>"
                + "</body></html>";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
                .header("Cache-Control", "no-store")
                .body(html.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseEntity<byte[]> readableHtml(byte[] html) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
                .header("Cache-Control", "public, max-age=600")
                .body(html);
    }

    private byte[] readableHtmlBytes(String content) {
        String body = buildReadableBody(content);
        String html = "<!doctype html><html><meta charset='utf-8'/>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'/>"
                + "<body style='font-family:Arial,Helvetica,sans-serif;line-height:1.65;padding:18px;color:#222'>"
                + "<h3 style='margin:0 0 12px 0;'>原文（可读模式）</h3>"
                + "<div style='font-size:13px;color:#666;margin-bottom:10px;'>源站阻止内嵌，已自动切换为正文渲染。</div>"
                + "<article style='max-width:980px;margin:0 auto;'>"
                + body
                + "</article>"
                + "</body></html>";
        return html.getBytes(StandardCharsets.UTF_8);
    }

    private String buildReadableBody(String content) {
        if (StringUtils.isBlank(content)) {
            return "<p style='color:#999'>（未抓取到正文）</p>";
        }
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");
        StringBuilder out = new StringBuilder();
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty()) {
                out.append("<div style='height:8px'></div>");
                continue;
            }
            String safe = line.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            if (isLikelyHeading(line)) {
                out.append("<h4 style='margin:14px 0 8px;font-size:16px;line-height:1.45;'>")
                        .append(safe)
                        .append("</h4>");
            } else if (isLikelyNumbered(line)) {
                out.append("<p style='margin:8px 0;text-indent:0;'>")
                        .append(safe)
                        .append("</p>");
            } else {
                out.append("<p style='margin:8px 0;text-indent:2em;'>")
                        .append(safe)
                        .append("</p>");
            }
        }
        return out.toString();
    }

    private boolean isLikelyHeading(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        return upper.startsWith("STATE OF ")
                || upper.startsWith("IN THE COURT")
                || upper.startsWith("DECISION")
                || upper.startsWith("JUDGMENT")
                || upper.startsWith("OPINION")
                || upper.startsWith("BACKGROUND")
                || upper.startsWith("ANALYSIS")
                || upper.startsWith("CONCLUSION");
    }

    private boolean isLikelyNumbered(String line) {
        return line.matches("^\\{?\\d+\\}?[\\.).、].*")
                || line.matches("^[IVXLC]+[\\.).].*")
                || line.matches("^\\(\\d+\\).*");
    }

    private ResponseEntity<byte[]> htmlError(String msg) {
        String html = "<!doctype html><html><meta charset='utf-8'/>"
                + "<body style='font-family:Arial;padding:16px;color:#444'>"
                + msg
                + "</body></html>";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
                .body(html.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/qa", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<String> caseQa(@RequestBody CaseQaReqVO vo) {
        try {
            checkReqVOService.checkCaseQaReqVO(vo);
            String summary = caseDetailService.getContent(vo.getCaseId(), vo.getLanguage());
            if (StringUtils.isBlank(summary)) {
                return JSONReturnBean.failed("请先生成 AI 摘要后再提问");
            }
            String answer = springAIService.answerCaseQuestion(summary, vo.getQuestion(), vo.getLanguage());
            return JSONReturnBean.success(answer, "success");
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("caseQa", e);
            return JSONReturnBean.failed("问答失败");
        }
    }

    @GetMapping("/favorites")
    public JSONReturnBean<FavoriteCaseResVO> queryFavoriteCases(@ModelAttribute QueryFavoriteCaseReqVO reqVO) {
        try {
            checkReqVOService.checkQueryFavoriteCaseVO(reqVO);
            FavoriteCaseResVO resVO = favoriteService.queryFavoriteCases(reqVO);
            return JSONReturnBean.success(resVO);
        } catch (ServiceException e) {
            log.error("查看收藏案例异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("查看收藏案例错误，请稍后再试，error=", e);
            return JSONReturnBean.failed("查看收藏案例错误，请稍后再试");
        }
    }

}
