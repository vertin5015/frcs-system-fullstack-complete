package com.hnu.legal_cases.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnu.legal_cases.config.CrawlerProperties;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.enums.CountryEnum;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
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
import java.net.http.HttpResponse;

/**
 * 内置 US / EU / JPN 列表与详情桥：优先调用官方 API，失败时回退到公开页面解析。
 */
@Slf4j
@Service
public class EuJpLegalSearchBridgeService {

    /** 日本最高裁英語判例検索（2024 年以降のサイト構造）；旧 /app/hanrei_* はリダイレクトでフォームのみ返ることがある */
    private static final String JP_SC_EN_SEARCH_INDEX = "https://www.courts.go.jp/english/Judgments/search/index.html";
    private static final String COURT_LISTENER_SEARCH = "https://www.courtlistener.com/";

    private static final Duration CONNECT = Duration.ofSeconds(8);
    private static final Duration READ = Duration.ofSeconds(55);
    /** SPARQL 查询不能占用搜索锁太久；超时就直接返回空结果，避免“系统繁忙”连锁反应。 */
    private static final Duration SPARQL_READ = Duration.ofSeconds(15);
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/120.0.0.0 Safari/537.36";
    private static final int MAX_ITEMS = 20;
    /** Eur-Lex / Curia URLs 中出现的 CELEX 案号片段 */
    private static final Pattern CELEX_TOKEN = Pattern.compile("(6\\d{4}[A-Z]{1,10}\\d{1,14})");
    private static final Pattern COURT_LISTENER_OPINION_ID = Pattern.compile("/opinion/(\\d+)/");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CrawlerProperties crawlerProperties;

    public List<CrawlerBaseInfoItem> searchUs(String keyword, String year) {
        List<CrawlerBaseInfoItem> official = searchCourtListenerApi(keyword, year);
        if (!official.isEmpty()) {
            return official;
        }
        return searchCourtListenerHtml(keyword, year);
    }

    public List<CrawlerBaseInfoItem> searchEu(String keyword, String year) {
        List<CrawlerBaseInfoItem> official = searchEurLexSparql(keyword, year);
        if (!official.isEmpty()) {
            return official;
        }
        // Eur-Lex 旧版 search.html 现已返回 404，继续回退只会拖慢搜索并制造无意义日志；
        // SPARQL 无结果时直接返回空结果。
        log.info("EUR-Lex SPARQL no results for keyword={}", keyword);
        return List.of();
    }

    private List<CrawlerBaseInfoItem> searchCourtListenerHtml(String keyword, String year) {
        if (keyword == null || keyword.isBlank()) {
            keyword = "contract";
        }
        String enc = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
        String filedAfter = "";
        if (year != null && !year.isBlank()) {
            try {
                int years = Math.max(1, Math.min(50, Integer.parseInt(year.trim())));
                filedAfter = "&filed_after=" + LocalDate.now().minusYears(years).withDayOfYear(1);
            } catch (NumberFormatException ignored) {
                filedAfter = "";
            }
        }
        String[] urls = {
                COURT_LISTENER_SEARCH + "?q=" + enc + "&type=o&order_by=score%20desc&stat_Published=on" + filedAfter,
                COURT_LISTENER_SEARCH + "?q=" + enc + "&type=o&order_by=score%20desc"
        };
        for (String url : urls) {
            try {
                String html = get(url);
                List<CrawlerBaseInfoItem> items = parseCourtListenerSearch(html);
                if (!items.isEmpty()) {
                    return items;
                }
            } catch (Exception e) {
                log.warn("US bridge fetch failed url={} : {}", url, e.getMessage());
            }
        }
        return List.of();
    }

    private List<CrawlerBaseInfoItem> searchEurLexHtml(String keyword) {
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

    private List<CrawlerBaseInfoItem> searchCourtListenerApi(String keyword, String year) {
        try {
            if (keyword == null || keyword.isBlank()) {
                keyword = "contract";
            }
            String base = crawlerProperties.getCourtListenerApiBaseUrl().trim().replaceAll("/+$", "");
            StringBuilder qs = new StringBuilder()
                    .append("?q=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8))
                    .append("&type=o&order_by=score%20desc&page_size=20");
            if (year != null && !year.isBlank()) {
                try {
                    int years = Math.max(1, Math.min(50, Integer.parseInt(year.trim())));
                    qs.append("&filed_after=")
                            .append(LocalDate.now().minusYears(years).withDayOfYear(1));
                } catch (NumberFormatException ignored) {
                    // ignore invalid year
                }
            }
            HttpResponse<String> response = requestJsonGet(base + "/search/" + qs);
            if (response.statusCode() != 200) {
                log.warn("CourtListener API search returned HTTP {}", response.statusCode());
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");
            if (!results.isArray()) {
                return List.of();
            }
            List<CrawlerBaseInfoItem> out = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (JsonNode row : results) {
                String title = text(row, "caseName");
                if (title.isBlank()) {
                    continue;
                }
                String clusterId = text(row, "cluster_id");
                String docket = clusterId.isBlank() ? "" : "CL-" + clusterId;
                if (docket.isBlank()) {
                    docket = "CL-" + text(row, "docketNumber");
                }
                String absUrl = text(row, "absolute_url");
                String url = absUrl;
                if (!absUrl.startsWith("http")) {
                    url = "https://www.courtlistener.com" + absUrl;
                }
                if (!seen.add(url)) {
                    continue;
                }
                String snippet = "";
                JsonNode opinions = row.path("opinions");
                if (opinions.isArray() && opinions.size() > 0) {
                    snippet = text(opinions.get(0), "snippet");
                }
                CrawlerBaseInfoItem item = new CrawlerBaseInfoItem();
                item.setSourceId(CountryEnum.US.getSourceId());
                item.setDocketNumber(docket);
                item.setTitle(title.length() > 512 ? title.substring(0, 512) : title);
                item.setUrl(url);
                item.setCitationCount(String.valueOf(row.path("citeCount").asInt(0)));
                String date = text(row, "dateFiled");
                item.setDateFiled(date.isBlank() ? LocalDate.now().withDayOfMonth(1).toString() : date);
                item.setSummary(snippet.isBlank() ? null : snippet);
                out.add(item);
                if (out.size() >= MAX_ITEMS) {
                    break;
                }
            }
            log.info("CourtListener API returned {} items", out.size());
            return out;
        } catch (Exception e) {
            log.warn("CourtListener API search failed, fallback to HTML: {}", e.getMessage());
            return List.of();
        }
    }

    private List<CrawlerBaseInfoItem> searchEurLexSparql(String keyword, String year) {
        try {
            if (keyword == null || keyword.isBlank()) {
                keyword = "judgment";
            }
            String safeKeyword = keyword.trim()
                    .replace("\\", " ")
                    .replace("\"", " ")
                    .replace("'", " ")
                    .replace("\n", " ")
                    .replace("\r", " ")
                    .toLowerCase(Locale.ROOT)
                    .trim();
            if (safeKeyword.isEmpty()) {
                return List.of();
            }

            StringBuilder query = new StringBuilder();
            query.append("PREFIX cdm: <http://publications.europa.eu/ontology/cdm#> ")
                    .append("SELECT DISTINCT ?work ?celex ?ecli ?date ?title ?parties WHERE { ")
                    .append("?work cdm:work_has_resource-type ?type . ")
                    .append("FILTER(?type = <http://publications.europa.eu/resource/authority/resource-type/JUDG>) ")
                    .append("OPTIONAL { ?work cdm:resource_legal_id_celex ?celex . } ")
                    .append("OPTIONAL { ?work cdm:case-law_ecli ?ecli . } ")
                    .append("OPTIONAL { ?work cdm:work_date_document ?date . } ")
                    .append("OPTIONAL { ?expr cdm:expression_belongs_to_work ?work . ?expr cdm:expression_title ?title . FILTER(lang(?title) = \"en\") } ")
                    .append("OPTIONAL { ?expr2 cdm:expression_belongs_to_work ?work . ?expr2 cdm:expression_case-law_parties ?parties . FILTER(lang(?parties) = \"en\") } ")
                    .append("FILTER(");
            String[] tokens = safeKeyword.split("\\s+");
            boolean hasTokenCondition = false;
            for (String token : tokens) {
                if (token.isBlank()) {
                    continue;
                }
                if (hasTokenCondition) {
                    query.append(" || ");
                }
                query.append("(CONTAINS(LCASE(STR(?title)), \"")
                        .append(token)
                        .append("\") || CONTAINS(LCASE(STR(?parties)), \"")
                        .append(token)
                        .append("\"))");
                hasTokenCondition = true;
            }
            query.append(") ");
            if (year != null && !year.isBlank()) {
                try {
                    int years = Math.max(1, Math.min(50, Integer.parseInt(year.trim())));
                    String from = LocalDate.now().minusYears(years).withDayOfYear(1).toString();
                    query.append("FILTER(!BOUND(?date) || ?date >= \"")
                            .append(from)
                            .append("\"^^<http://www.w3.org/2001/XMLSchema#date>) ");
                } catch (NumberFormatException ignored) {
                    // ignore invalid year
                }
            }
            query.append("FILTER not exists { ?work cdm:do_not_index \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> . } ")
                    .append("} LIMIT 20");

            String sparqlUrl = crawlerProperties.getEurlexSparqlUrl().trim();
            URI sparqlUri = URI.create(sparqlUrl);
            assertAllowedHost(sparqlUri);
            String formBody = "query=" + URLEncoder.encode(query.toString(), StandardCharsets.UTF_8)
                    + "&format=" + URLEncoder.encode("application/sparql-results+json", StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(sparqlUri)
                    .timeout(SPARQL_READ)
                    .header("User-Agent", UA)
                    .header("Accept", "application/sparql-results+json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("EUR-Lex SPARQL returned HTTP {}", response.statusCode());
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode bindings = root.path("results").path("bindings");
            if (!bindings.isArray()) {
                return List.of();
            }
            List<CrawlerBaseInfoItem> out = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (JsonNode binding : bindings) {
                String celex = bindingText(binding, "celex");
                if (celex.isBlank()) {
                    continue;
                }
                if (!seen.add(celex)) {
                    continue;
                }
                String title = bindingText(binding, "title");
                String parties = bindingText(binding, "parties");
                if (title.isBlank()) {
                    title = parties;
                }
                if (title.isBlank()) {
                    title = celex;
                }
                String url = "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri="
                        + URLEncoder.encode("CELEX:" + celex, StandardCharsets.UTF_8);
                CrawlerBaseInfoItem item = new CrawlerBaseInfoItem();
                item.setSourceId(CountryEnum.EU.getSourceId());
                item.setDocketNumber(celex);
                item.setTitle(title.length() > 512 ? title.substring(0, 512) : title);
                item.setUrl(url);
                item.setCitationCount("0");
                String date = bindingText(binding, "date");
                item.setDateFiled(date.isBlank() ? LocalDate.now().withDayOfMonth(1).toString() : date);
                item.setSummary(title);
                out.add(item);
                if (out.size() >= MAX_ITEMS) {
                    break;
                }
            }
            log.info("EUR-Lex SPARQL returned {} items", out.size());
            return out;
        } catch (Exception e) {
            log.warn("EUR-Lex SPARQL search failed, fallback to HTML: {}", e.getMessage());
            return List.of();
        }
    }

    public List<CrawlerBaseInfoItem> searchJp(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            keyword = "contract";
        }
        String enc = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
        String[] urls = {
                JP_SC_EN_SEARCH_INDEX + "?query1=" + enc,
        };
        for (String url : urls) {
            try {
                String html = get(url);
                List<CrawlerBaseInfoItem> items = parseCourtsJpModernEnglishJudgments(html, JP_SC_EN_SEARCH_INDEX);
                if (!items.isEmpty()) {
                    return items;
                }
            } catch (Exception e) {
                log.warn("JPN bridge fetch failed url={} : {}", url, e.getMessage());
            }
        }
        // 不做 "judgment" 兜底搜索：搜不到真实匹配时应返回空结果，
        // 否则用户会看到与关键词无关的日本案例。
        log.info("JPN bridge: no results for keyword={}", keyword);
        return List.of();
    }

    private String get(String urlStr) throws IOException, InterruptedException {
        HttpResponse<String> resp = request(urlStr);
        int sc = resp.statusCode();
        if (sc != 200) {
            throw new IOException("HTTP " + sc);
        }
        return resp.body();
    }

    private HttpResponse<String> request(String urlStr) throws IOException, InterruptedException {
        URI uri = URI.create(urlStr);
        assertAllowedHost(uri);
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(READ)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,ja;q=0.75")
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> requestJsonGet(String urlStr) throws IOException, InterruptedException {
        URI uri = URI.create(urlStr);
        assertAllowedHost(uri);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(READ)
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9");
        String apiKey = crawlerProperties.getCourtListenerApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Token " + apiKey.trim());
        }
        return http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertAllowedHost(URI uri) throws IOException {
        String host = uri.getHost();
        if (host == null || (!host.endsWith("europa.eu")
                && !host.endsWith("eur-lex.europa.eu")
                && !host.endsWith("courts.go.jp")
                && !host.endsWith("courtlistener.com"))) {
            throw new IOException("disallowed host");
        }
    }

    static boolean isDeferredResponse(int statusCode) {
        return statusCode == 202;
    }

    public String fetchDetail(String detailUrl) throws IOException, InterruptedException {
        if (detailUrl == null || detailUrl.isBlank()) {
            return "";
        }
        String normalized = detailUrl.trim();
        if (normalized.contains("courtlistener.com")) {
            return fetchCourtListenerDetail(normalized);
        }
        if (normalized.contains("eur-lex.europa.eu")
                || normalized.contains("publications.europa.eu")
                || normalized.toUpperCase(Locale.ROOT).contains("CELEX:")) {
            return fetchEurLexDetail(normalized);
        }
        return fetchHtmlDetail(normalized);
    }

    private String fetchCourtListenerDetail(String detailUrl) throws IOException, InterruptedException {
        String clusterId = "";
        Matcher idMatcher = COURT_LISTENER_OPINION_ID.matcher(detailUrl);
        if (idMatcher.find()) {
            clusterId = idMatcher.group(1);
        }
        if (clusterId.isBlank()) {
            return fetchHtmlDetail(detailUrl);
        }
        String apiKey = crawlerProperties.getCourtListenerApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("CourtListener 未配置 API token，无法获取全文；请配置 COURTLISTENER_API_KEY");
        }
        String base = crawlerProperties.getCourtListenerApiBaseUrl().trim().replaceAll("/+$", "");
        StringBuilder apiFailure = new StringBuilder();

        /*
         * 站点 URL 里的编号是 cluster_id，不是 opinion_id；而 CourtListener v4 只接受 filterset
         * 中声明的过滤参数，未知参数会直接返回 HTTP 400（Unknown filter parameters are not allowed）。
         * 因此这里必须用 cluster__id（双下划线），cluster_id 会被判为未知参数。
         */
        String detailQuery = buildCourtListenerOpinionsQuery(clusterId);
        URI detailUri = URI.create(base + "/opinions/" + detailQuery);
        assertAllowedHost(detailUri);
        HttpResponse<String> response = courtListenerGet(detailUri, apiKey);
        int statusCode = response.statusCode();
        if (statusCode == 401 || statusCode == 403) {
            throw new IOException("CourtListener API token 无效（HTTP " + statusCode
                    + "），请检查 COURTLISTENER_API_KEY");
        }
        if (statusCode == 429) {
            throw new IOException("CourtListener 原文接口限流（HTTP 429），请稍后重试或减少访问频率");
        }
        if (statusCode == 200) {
            String text = extractCourtListenerOpinionListText(response.body(), detailUrl);
            if (!text.isBlank()) {
                return capText(text);
            }
            apiFailure.append("opinions 接口未返回正文");
        } else {
            String apiDetail = extractCourtListenerApiError(response.body());
            apiFailure.append("opinions 接口 HTTP ").append(statusCode);
            if (!apiDetail.isBlank()) {
                apiFailure.append("（").append(apiDetail).append("）");
            }
        }
        log.warn("CourtListener opinions?cluster__id= 未取到正文（{}），改用 Cluster API cluster_id={}",
                apiFailure, clusterId);

        // 官方文档「Finding a Case by URL」推荐路径：cluster 详情里的 sub_opinions。
        try {
            String clusterText = fetchCourtListenerClusterText(base, apiKey, clusterId, detailUrl);
            if (!clusterText.isBlank()) {
                return capText(clusterText);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            log.warn("CourtListener Cluster API 失败: {}", e.getMessage());
        }

        // 最后回退抓 HTML 页面：CourtListener 对 /opinion/ 页面会返回 202 防爬挑战。
        try {
            return fetchHtmlDetail(detailUrl);
        } catch (IOException e) {
            if (apiFailure.length() > 0) {
                throw new IOException(apiFailure + "；" + e.getMessage());
            }
            throw e;
        }
    }

    private HttpResponse<String> courtListenerGet(URI uri, String apiKey) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(READ)
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .header("Authorization", "Token " + apiKey.trim())
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /**
     * CourtListener v4 会拒绝 filterset 未声明的过滤参数（HTTP 400 Unknown filter parameters）。
     * opinion URL 中的编号是 cluster_id，opinions 接口里对应的过滤参数是 {@code cluster__id}（双下划线）。
     */
    static String buildCourtListenerOpinionsQuery(String clusterId) {
        return "?cluster__id=" + URLEncoder.encode(clusterId, StandardCharsets.UTF_8) + "&page_size=10";
    }

    /**
     * 解析 {@code /opinions/?cluster__id=xxx} 的列表响应，按顺序拼接所有 opinion 的正文。
     */
    private String extractCourtListenerOpinionListText(String body, String detailUrl) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("results");
            if (!results.isArray()) {
                return "";
            }
            StringBuilder out = new StringBuilder();
            for (JsonNode opinion : results) {
                String opinionText = extractCourtListenerOpinionText(opinion, detailUrl);
                if (!opinionText.isBlank()) {
                    if (out.length() > 0) {
                        out.append("\n\n");
                    }
                    out.append(opinionText);
                }
            }
            return out.toString().trim();
        } catch (Exception e) {
            log.warn("CourtListener opinions 响应解析失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 回退方案：GET /clusters/{clusterId}/，再逐个 GET sub_opinions 里的 opinion 详情。
     */
    private String fetchCourtListenerClusterText(String base, String apiKey, String clusterId, String detailUrl)
            throws IOException, InterruptedException {
        URI clusterUri = URI.create(base + "/clusters/"
                + URLEncoder.encode(clusterId, StandardCharsets.UTF_8) + "/");
        assertAllowedHost(clusterUri);
        HttpResponse<String> response = courtListenerGet(clusterUri, apiKey);
        if (response.statusCode() != 200) {
            log.warn("CourtListener Cluster API 返回 HTTP {} cluster_id={}", response.statusCode(), clusterId);
            return "";
        }
        JsonNode cluster;
        try {
            cluster = objectMapper.readTree(response.body());
        } catch (Exception e) {
            log.warn("CourtListener Cluster API 响应解析失败: {}", e.getMessage());
            return "";
        }
        JsonNode subOpinions = cluster.path("sub_opinions");
        if (!subOpinions.isArray() || subOpinions.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (JsonNode node : subOpinions) {
            String opinionUrl = node.isTextual() ? node.asText("") : text(node, "resource_uri");
            if (opinionUrl.isBlank()) {
                continue;
            }
            URI opinionUri = URI.create(opinionUrl);
            assertAllowedHost(opinionUri);
            HttpResponse<String> opinionResponse = courtListenerGet(opinionUri, apiKey);
            if (opinionResponse.statusCode() != 200) {
                continue;
            }
            try {
                JsonNode opinion = objectMapper.readTree(opinionResponse.body());
                String opinionText = extractCourtListenerOpinionText(opinion, detailUrl);
                if (!opinionText.isBlank()) {
                    if (out.length() > 0) {
                        out.append("\n\n");
                    }
                    out.append(opinionText);
                }
            } catch (Exception e) {
                log.warn("CourtListener opinion 详情解析失败 url={}: {}", opinionUrl, e.getMessage());
            }
        }
        return out.toString().trim();
    }

    private String extractCourtListenerApiError(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String detail = text(root, "detail");
            if (!detail.isBlank()) {
                JsonNode unknown = root.path("unknown_params");
                if (unknown.isArray() && !unknown.isEmpty()) {
                    List<String> params = new ArrayList<>();
                    unknown.forEach(node -> params.add(node.asText("")));
                    return detail + " unknown_params=" + String.join(",", params);
                }
                return detail;
            }
        } catch (Exception ignored) {
            // 非 JSON 响应体
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() > 200 ? compact.substring(0, 200) : compact;
    }

    private String extractCourtListenerOpinionText(JsonNode opinion, String detailUrl) {
        String htmlWithCitations = text(opinion, "html_with_citations");
        if (!htmlWithCitations.isBlank()) {
            return extractHtmlParagraphs(htmlWithCitations, detailUrl);
        }
        String html = text(opinion, "html");
        if (!html.isBlank()) {
            return extractHtmlParagraphs(html, detailUrl);
        }
        String plainText = text(opinion, "plain_text");
        if (!plainText.isBlank()) {
            return normalizeTextBlocks(plainText);
        }
        String xmlHarvard = text(opinion, "xml_harvard");
        if (!xmlHarvard.isBlank()) {
            String parsed = extractHtmlParagraphs(xmlHarvard, detailUrl);
            return parsed.isBlank() ? normalizeTextBlocks(xmlHarvard) : parsed;
        }
        return "";
    }

    private String extractHtmlParagraphs(String html, String baseUri) {
        Document doc = Jsoup.parse(html, baseUri);
        doc.select("script,style,noscript,header,footer,nav,form").remove();
        Element root = doc.body() == null ? doc : doc.body();
        String text = extractParagraphText(root);
        return text.isBlank() ? normalizeTextBlocks(root.text()) : text;
    }

    private String fetchEurLexDetail(String detailUrl) throws IOException, InterruptedException {
        String celex = extractCelexToken(detailUrl);
        if (celex.isBlank()) {
            return fetchHtmlDetail(detailUrl);
        }
        try {
            String base = crawlerProperties.getEurlexContentBaseUrl().trim().replaceAll("/+$", "");
            String contentUrl = base + "/" + URLEncoder.encode(celex, StandardCharsets.UTF_8) + "?lang=en";
            URI uri = URI.create(contentUrl);
            HttpResponse<String> response = requestContentFollowingRedirects(uri);
            if (response.statusCode() != 200) {
                log.warn("EUR-Lex content HTTP {}; fallback to HTML detail", response.statusCode());
                return fetchHtmlDetail(detailUrl);
            }
            Document doc = Jsoup.parse(response.body(), contentUrl);
            doc.select("script,style,noscript,header,footer,nav,form").remove();
            Element textOnly = doc.selectFirst("#TexteOnly");
            Element root = textOnly == null ? (doc.body() == null ? doc : doc.body()) : textOnly;
            String text = extractParagraphText(root);
            if (text.isBlank()) {
                text = normalizeTextBlocks(root.text());
            }
            return capText(text);
        } catch (Exception e) {
            log.warn("EUR-Lex content fetch failed, fallback to HTML: {}", e.getMessage());
            return fetchHtmlDetail(detailUrl);
        }
    }

    private HttpResponse<String> requestContentFollowingRedirects(URI uri) throws IOException, InterruptedException {
        URI current = uri;
        for (int i = 0; i < 3; i++) {
            assertAllowedHost(current);
            HttpRequest req = HttpRequest.newBuilder(current)
                    .timeout(READ)
                    .header("User-Agent", UA)
                    .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status != 301 && status != 302 && status != 303 && status != 307 && status != 308) {
                return response;
            }
            String location = response.headers().firstValue("Location").orElse("");
            if (location.isBlank()) {
                return response;
            }
            URI next = current.resolve(location);
            if (next.equals(current)) {
                return response;
            }
            current = next;
        }
        return requestContent(current, "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8", "en");
    }

    private HttpResponse<String> requestContent(URI uri, String accept, String acceptLanguage)
            throws IOException, InterruptedException {
        assertAllowedHost(uri);
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(READ)
                .header("User-Agent", UA)
                .header("Accept", accept)
                .header("Accept-Language", acceptLanguage)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String fetchHtmlDetail(String detailUrl) throws IOException, InterruptedException {
        HttpResponse<String> response = requestDetailWithRetry(detailUrl.trim());
        if (isDeferredResponse(response.statusCode())) {
            throw new IOException("目标站点触发防爬（HTTP 202），请稍后重试或外部打开原文");
        }
        if (response.statusCode() != 200) {
            throw new IOException("目标站点返回 HTTP " + response.statusCode());
        }
        String html = response.body();
        Document doc = Jsoup.parse(html, detailUrl);
        doc.select("script,style,noscript,header,footer,nav,form").remove();

        if (detailUrl.contains("courts.go.jp")) {
            String japanText = extractJapanJudgmentText(doc);
            if (!japanText.isBlank()) {
                return capText(japanText);
            }
        }

        String title = str(doc.title());
        Element main = firstPresent(doc,
                ".opinion-content",
                ".plaintext",
                ".opinion",
                "#opinion",
                "article",
                "main",
                ".content");
        Element root = main == null ? (doc.body() == null ? doc : doc.body()) : main;
        String text = extractParagraphText(root);
        if (text.isBlank()) {
            text = normalizeTextBlocks(root.text());
        }
        if (text.length() > 120_000) {
            text = text.substring(0, 120_000);
        }
        if (title.isEmpty()) {
            return text;
        }
        return title + "\n\n" + text;
    }

    private String extractJapanJudgmentText(Document doc) {
        Elements blocks = doc.select("div.module-sub-page-parts-table dl");
        if (blocks.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Element block : blocks) {
            Element dt = block.selectFirst("dt");
            if (dt == null) {
                continue;
            }
            String label = normalizeWhitespace(dt.wholeText());
            if (label.isBlank()) {
                continue;
            }
            out.append(label).append("\n");
            for (Element p : block.select("dd > p")) {
                String content = normalizePreserveLineBreaks(p.wholeText());
                if (!content.isBlank()) {
                    out.append(content).append("\n");
                }
            }
            out.append("\n");
        }
        return out.toString().trim();
    }

    private String normalizePreserveLineBreaks(String raw) {
        if (raw == null) {
            return "";
        }
        String[] lines = raw.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            String value = normalizeWhitespace(line);
            if (!value.isBlank()) {
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append(value);
            }
        }
        return out.toString();
    }

    private String extractParagraphText(Element root) {
        if (root == null) {
            return "";
        }
        StringBuilder current = new StringBuilder();
        List<String> paragraphs = new ArrayList<>();
        walkTextNodes(root, current, paragraphs);
        flushParagraph(current, paragraphs);
        return String.join("\n\n", paragraphs);
    }

    private void walkTextNodes(Node node, StringBuilder current, List<String> paragraphs) {
        if (node instanceof TextNode textNode) {
            String text = normalizeWhitespace(textNode.text());
            if (!text.isBlank()) {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(text);
            }
            return;
        }
        if (!(node instanceof Element element)) {
            return;
        }
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        if ("br".equals(tag)) {
            flushParagraph(current, paragraphs);
            return;
        }
        boolean block = BLOCK_TAGS.contains(tag);
        if (block && current.length() > 0) {
            flushParagraph(current, paragraphs);
        }
        for (Node child : element.childNodes()) {
            walkTextNodes(child, current, paragraphs);
        }
        if (block && current.length() > 0) {
            flushParagraph(current, paragraphs);
        }
    }

    private void flushParagraph(StringBuilder current, List<String> paragraphs) {
        String text = normalizeWhitespace(current.toString());
        if (!text.isBlank()) {
            if (paragraphs.isEmpty() || !paragraphs.get(paragraphs.size() - 1).equals(text)) {
                paragraphs.add(text);
            }
        }
        current.setLength(0);
    }

    private static final Set<String> BLOCK_TAGS = Set.of(
            "h1", "h2", "h3", "h4", "h5", "h6", "p", "li", "blockquote", "pre",
            "div", "section", "article", "table", "thead", "tbody", "tfoot", "tr");

    private String capText(String text) {
        String value = text == null ? "" : text.trim();
        int cap = crawlerProperties.getMaxDetailItemChars();
        if (cap > 0 && value.length() > cap) {
            return value.substring(0, cap) + "\n\n[truncated: " + (value.length() - cap) + " chars omitted]";
        }
        return value;
    }

    private String normalizeTextBlocks(String text) {
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n").trim();
        String[] blocks = normalized.split("\\n{2,}");
        List<String> out = new ArrayList<>();
        for (String block : blocks) {
            String value = block.trim();
            if (!value.isBlank()) {
                out.add(value.replace('\u00a0', ' '));
            }
        }
        return String.join("\n\n", out);
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String extractCelexToken(String url) {
        String upper = url.toUpperCase(Locale.ROOT);
        Matcher m = CELEX_TOKEN.matcher(upper);
        if (m.find()) {
            return m.group(1);
        }
        m = Pattern.compile("(?:CELEX[:\\s%]*|RESOURCE/CELEX/)(\\d{4}[A-Z]{1,10}\\d{1,14})")
                .matcher(upper);
        return m.find() ? m.group(1) : "";
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || value.isMissingNode() ? "" : value.asText("");
    }

    private static String bindingText(JsonNode binding, String field) {
        JsonNode value = binding == null ? null : binding.path(field);
        if (value == null || value.isMissingNode()) {
            return "";
        }
        JsonNode literal = value.get("value");
        return literal == null || literal.isNull() || literal.isMissingNode() ? "" : literal.asText("");
    }

    private HttpResponse<String> requestDetailWithRetry(String detailUrl)
            throws IOException, InterruptedException {
        HttpResponse<String> response = request(detailUrl);
        if (!isDeferredResponse(response.statusCode())) {
            return response;
        }
        long delayMs = response.headers().firstValue("Retry-After")
                .flatMap(value -> parseRetryAfterMillis(value))
                .orElse(1_000L);
        Thread.sleep(Math.min(delayMs, 2_000L));
        return request(detailUrl);
    }

    private static java.util.Optional<Long> parseRetryAfterMillis(String value) {
        try {
            long seconds = Long.parseLong(value.trim());
            return java.util.Optional.of(Math.max(0L, seconds * 1_000L));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }

    private List<CrawlerBaseInfoItem> parseCourtListenerSearch(String html) {
        Document doc = Jsoup.parse(html, COURT_LISTENER_SEARCH);
        Set<String> seen = new LinkedHashSet<>();
        List<CrawlerBaseInfoItem> out = new ArrayList<>();
        for (Element a : doc.select("a[href*=/opinion/]")) {
            String href = normalizeCourtListenerUrl(a.absUrl("href"));
            Matcher idMatcher = COURT_LISTENER_OPINION_ID.matcher(href);
            if (!idMatcher.find()) {
                continue;
            }
            String opinionId = idMatcher.group(1);
            if (!seen.add(opinionId)) {
                continue;
            }
            String title = str(a.text());
            if (title.isEmpty() || title.length() < 4) {
                Element parent = a.parent();
                title = parent == null ? "" : str(parent.text());
            }
            if (title.length() > 512) {
                title = title.substring(0, 512);
            }
            if (title.isEmpty()) {
                title = "CourtListener opinion " + opinionId;
            }

            CrawlerBaseInfoItem it = new CrawlerBaseInfoItem();
            it.setSourceId(CountryEnum.US.getSourceId());
            it.setDocketNumber("CL-" + opinionId);
            it.setTitle(title);
            it.setUrl(href);
            it.setCitationCount("0");
            it.setDateFiled(extractDateNear(a));
            out.add(it);
            if (out.size() >= MAX_ITEMS) {
                break;
            }
        }
        return out;
    }

    private static String normalizeCourtListenerUrl(String href) {
        if (href == null) {
            return "";
        }
        int q = href.indexOf('?');
        if (q > 0) {
            href = href.substring(0, q);
        }
        int hash = href.indexOf('#');
        if (hash > 0) {
            href = href.substring(0, hash);
        }
        return href;
    }

    private static String extractDateNear(Element a) {
        Element cur = a;
        for (int i = 0; i < 4 && cur != null; i++) {
            String text = cur.text();
            Matcher m = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(text);
            if (m.find()) {
                return m.group(1) + "-" + m.group(2) + "-" + m.group(3);
            }
            m = Pattern.compile("([A-Z][a-z]{2,8})\\s+(\\d{1,2}),\\s+(\\d{4})").matcher(text);
            if (m.find()) {
                return m.group(3) + "-01-01";
            }
            cur = cur.parent();
        }
        return LocalDate.now().withDayOfMonth(1).toString();
    }

    private static Element firstPresent(Document doc, String... selectors) {
        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !str(el.text()).isEmpty()) {
                return el;
            }
        }
        return null;
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
