package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.enums.CountryEnum;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 内置 EU / JPN 列表搜索：直连官方公开页面解析（无外网爬虫进程时启用）。
 */
@Slf4j
@Service
public class EuJpLegalSearchBridgeService {

    /** 日本最高裁英語判例検索（2024 年以降のサイト構造）；旧 /app/hanrei_* はリダイレクトでフォームのみ返ることがある */
    private static final String JP_SC_EN_SEARCH_INDEX = "https://www.courts.go.jp/english/Judgments/search/index.html";

    private static final Duration CONNECT = Duration.ofSeconds(8);
    private static final Duration READ = Duration.ofSeconds(55);
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/120.0.0.0 Safari/537.36";
    private static final int MAX_ITEMS = 20;
    /** Eur-Lex / Curia URLs 中出现的 CELEX 案号片段 */
    private static final Pattern CELEX_TOKEN = Pattern.compile("(6\\d{4}[A-Z]{1,10}\\d{1,14})");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<CrawlerBaseInfoItem> searchEu(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            keyword = "judgment";
        }
        String enc = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
        String[] urls = {
                "https://eur-lex.europa.eu/search.html?type=simple&lang=en&page=1&text=" + enc,
                "https://eur-lex.europa.eu/search.html?CASE_LAW_SUMMARY=true&lang=en&type=simple&page=1&text=" + enc,
        };
        for (String url : urls) {
            try {
                String html = get(url);
                if (looksLikeAwsWafInterstitial(html)) {
                    throw new IOException("Eur-Lex returned AWS WAF challenge page (not parseable server-side)");
                }
                List<CrawlerBaseInfoItem> items = parseEurLex(html);
                if (!items.isEmpty()) {
                    return items;
                }
            } catch (Exception e) {
                log.warn("EU bridge fetch failed url={} : {}", url, e.getMessage());
            }
        }
        log.info("EU bridge: no results — Eur-Lex often requires browser cookies / TLS (AWS WAF); try VPN, Scrapy crawler, or another region.");
        return List.of();
    }

    public List<CrawlerBaseInfoItem> searchJp(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            keyword = "contract";
        }
        String enc = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
        String[] urls = {
                JP_SC_EN_SEARCH_INDEX + "?query1=" + enc,
                "https://www.courts.go.jp/app/hanrei_en/search?page=1&q=" + enc,
                "https://www.courts.go.jp/app/hanrei_en/search?page=1&keyword=" + enc,
                "https://www.courts.go.jp/app/hanrei_en/search?page=1&queryText=" + enc,
                "https://www.courts.go.jp/app/hanrei_jp/search1?kw=" + enc,
                "https://www.courts.go.jp/app/hanrei_jp/search1?keyword=" + enc,
        };
        for (String url : urls) {
            try {
                String html = get(url);
                List<CrawlerBaseInfoItem> items;
                if (url.startsWith(JP_SC_EN_SEARCH_INDEX)) {
                    items = parseCourtsJpModernEnglishJudgments(html, JP_SC_EN_SEARCH_INDEX);
                } else {
                    items = parseCourtsJpLegacyHanrei(html);
                }
                if (!items.isEmpty()) {
                    return items;
                }
            } catch (Exception e) {
                log.warn("JPN bridge fetch failed url={} : {}", url, e.getMessage());
            }
        }
        try {
            String html = get(JP_SC_EN_SEARCH_INDEX + "?query1=" + URLEncoder.encode("judgment", StandardCharsets.UTF_8));
            return parseCourtsJpModernEnglishJudgments(html, JP_SC_EN_SEARCH_INDEX);
        } catch (Exception e) {
            log.warn("JPN bridge fallback supreme court english search failed: {}", e.getMessage());
            try {
                String html = get("https://www.courts.go.jp/app/hanrei_en/search?page=1");
                return parseCourtsJpLegacyHanrei(html);
            } catch (Exception e2) {
                log.warn("JPN bridge legacy fallback list failed: {}", e2.getMessage());
                return List.of();
            }
        }
    }

    private String get(String urlStr) throws IOException, InterruptedException {
        URI uri = URI.create(urlStr);
        String host = uri.getHost();
        if (host == null || (!host.endsWith("europa.eu") && !host.endsWith("eur-lex.europa.eu") && !host.endsWith("courts.go.jp"))) {
            throw new IOException("disallowed host");
        }
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(READ)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,ja;q=0.75")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int sc = resp.statusCode();
        /* AWS WAF 等对脚本返回 202 + 挑战页；爬虫必须拒绝，否则会误当成“无结果”。 */
        if (sc != 200) {
            throw new IOException("HTTP " + sc);
        }
        return resp.body();
    }

    private List<CrawlerBaseInfoItem> parseEurLex(String html) {
        Document doc = Jsoup.parse(html, "https://eur-lex.europa.eu/");
        Set<String> seen = new LinkedHashSet<>();
        List<CrawlerBaseInfoItem> out = new ArrayList<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.absUrl("href");
            if (!href.contains("legal-content") && !href.toLowerCase().contains("celex")) {
                continue;
            }
            String celex = extractCelex(href);
            if (celex == null) {
                continue;
            }
            if (!seen.add(celex)) {
                continue;
            }
            String title = str(a.text());
            if (title.length() > 512) {
                title = title.substring(0, 512);
            }
            if (title.isEmpty()) {
                title = celex;
            }
            CrawlerBaseInfoItem it = new CrawlerBaseInfoItem();
            it.setSourceId(CountryEnum.EU.getSourceId());
            it.setDocketNumber(celex);
            it.setTitle(title);
            it.setUrl(href);
            it.setCitationCount("0");
            String y = guessYear(celex);
            if (y != null) {
                it.setDateFiled(y + "-01-01");
            } else {
                it.setDateFiled(LocalDate.now().withDayOfMonth(1).toString());
            }
            out.add(it);
            if (out.size() >= MAX_ITEMS) {
                break;
            }
        }
        return out;
    }

    private static String guessYear(String celex) {
        if (celex == null) {
            return null;
        }
        Matcher m = Pattern.compile("6(\\d{4})").matcher(celex);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String extractCelex(String url) {
        Matcher m = CELEX_TOKEN.matcher(url);
        if (m.find()) {
            return "CELEX:" + m.group(1);
        }
        return null;
    }

    private static boolean looksLikeAwsWafInterstitial(String html) {
        if (html == null || html.length() > 30_000) {
            return false;
        }
        String h = html.toLowerCase(Locale.ROOT);
        return h.contains("awswafintegration")
                || h.contains("challenge.js")
                || h.contains("token.awswaf");
    }

    /**
     * www.courts.go.jp english/Judgments/search/index.html で返る「search-result-table」行を読む。
     */
    private List<CrawlerBaseInfoItem> parseCourtsJpModernEnglishJudgments(String html, String baseUri) {
        Document doc = Jsoup.parse(html, baseUri == null ? JP_SC_EN_SEARCH_INDEX : baseUri);
        Elements rows = doc.select("table.search-result-table tbody tr");
        Set<String> seen = new LinkedHashSet<>();
        List<CrawlerBaseInfoItem> out = new ArrayList<>();
        for (Element tr : rows) {
            Element a = tr.selectFirst("th a[href]");
            if (a == null) {
                continue;
            }
            String href = a.absUrl("href");
            if (!href.contains("courts.go.jp")) {
                continue;
            }
            String docket = str(a.text());
            if (docket.isEmpty()) {
                continue;
            }
            if (!seen.add(docket + "|" + href)) {
                continue;
            }
            String title = null;
            for (Element p : tr.select("td p")) {
                String t = str(p.text());
                if (t.regionMatches(true, 0, "Title:", 0, 6)) {
                    title = str(t.substring(6));
                    break;
                }
            }
            if (title == null || title.isEmpty()) {
                title = docket;
            }
            if (title.length() > 512) {
                title = title.substring(0, 512);
            }
            CrawlerBaseInfoItem it = new CrawlerBaseInfoItem();
            it.setSourceId(CountryEnum.JPN.getSourceId());
            it.setDocketNumber(docket.replaceAll("\\s+", " ").trim());
            it.setTitle(title);
            it.setUrl(href);
            it.setCitationCount("0");
            String filed = "";
            for (Element p : tr.select("td p")) {
                String t = str(p.text());
                if (t.regionMatches(true, 0, "Date of the judgment", 0, 22)) {
                    int colon = t.indexOf(':');
                    if (colon > 0 && colon + 1 < t.length()) {
                        filed = str(t.substring(colon + 1));
                        break;
                    }
                }
            }
            Matcher dm = Pattern.compile("(\\d{4})\\.(\\d{2})\\.(\\d{2})").matcher(filed);
            if (dm.find()) {
                it.setDateFiled(dm.group(1) + "-" + dm.group(2) + "-" + dm.group(3));
            } else {
                it.setDateFiled(LocalDate.now().withDayOfMonth(1).toString());
            }
            out.add(it);
            if (out.size() >= MAX_ITEMS) {
                break;
            }
        }
        return out;
    }

    private List<CrawlerBaseInfoItem> parseCourtsJpLegacyHanrei(String html) {
        Document doc = Jsoup.parse(html, "https://www.courts.go.jp/");
        Set<String> seen = new LinkedHashSet<>();
        List<CrawlerBaseInfoItem> out = new ArrayList<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.absUrl("href");
            if (!href.contains("courts.go.jp")) {
                continue;
            }
            if (!href.contains("/app/hanrei")) {
                continue;
            }
            String id = extractIdParam(href);
            if (id == null && !href.toLowerCase(Locale.ROOT).contains("detail")) {
                continue;
            }
            if (id == null) {
                id = href;
            }
            if (!seen.add(id)) {
                continue;
            }
            String title = str(a.text());
            if (title.length() > 512) {
                title = title.substring(0, 512);
            }
            if (title.isEmpty()) {
                title = "Japan court judgment";
            }
            CrawlerBaseInfoItem it = new CrawlerBaseInfoItem();
            it.setSourceId(CountryEnum.JPN.getSourceId());
            it.setDocketNumber("JP-" + Math.abs(id.hashCode()));
            it.setTitle(title);
            it.setUrl(href);
            it.setCitationCount("0");
            it.setDateFiled(LocalDate.now().withDayOfMonth(1).toString());
            out.add(it);
            if (out.size() >= MAX_ITEMS) {
                break;
            }
        }
        return out;
    }

    private static String str(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static String extractIdParam(String href) {
        Matcher m = Pattern.compile("[?&]id=([^&]+)").matcher(href);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
