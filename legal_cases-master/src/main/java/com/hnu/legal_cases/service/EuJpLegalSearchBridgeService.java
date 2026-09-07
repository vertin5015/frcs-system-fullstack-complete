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
        return searchEurLexHtml(keyword);
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
                    .append("FILTER(CONTAINS(LCASE(STR(?title)), \"")
                    .append(safeKeyword)
                    .append("\") || CONTAINS(LCASE(STR(?parties)), \"")
                    .append(safeKeyword)
                    .append("\")) ");
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
                    .timeout(READ)
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
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(READ)
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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

    private String fetchCourtListenerDetail(String detailUrl) {
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
            log.warn("CourtListener full text requires COURTLISTENER_API_KEY; fallback to HTML detail url={}", detailUrl);
            return fetchHtmlDetail(detailUrl);
        }
        try {
            String base = crawlerProperties.getCourtListenerApiBaseUrl().trim().replaceAll("/+$", "");
            String detailQuery = "?cluster_id=" + URLEncoder.encode(clusterId, StandardCharsets.UTF_8)
                    + "&page_size=10";
            URI detailUri = URI.create(base + "/opinions/" + detailQuery);
            assertAllowedHost(detailUri);
            HttpRequest req = HttpRequest.newBuilder(detailUri)
                    .timeout(READ)
                    .header("User-Agent", UA)
                    .header("Accept", "application/json")
                    .header("Authorization", "Token " + apiKey.trim())
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                log.warn("CourtListener API token rejected status={}; fallback to HTML detail", response.statusCode());
                return fetchHtmlDetail(detailUrl);
            }
            if (response.statusCode() != 200) {
                log.warn("CourtListener API detail HTTP {}; fallback to HTML detail", response.statusCode());
                return fetchHtmlDetail(detailUrl);
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode opinion : results) {
                    String opinionText = extractCourtListenerOpinionText(opinion, detailUrl);
                    if (!opinionText.isBlank()) {
                        return capText(opinionText);
                    }
                }
            }
            log.warn("CourtListener API detail has no text fields cluster_id={}", clusterId);
            return fetchHtmlDetail(detailUrl);
        } catch (Exception e) {
            log.warn("CourtListener API detail failed, fallback to HTML: {}", e.getMessage());
            return fetchHtmlDetail(detailUrl);
        }
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

    private String fetchEurLexDetail(String detailUrl) {
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
            URI next = URI.create(current.resolve(location));
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

    private String fetchHtmlDetail(String detailUrl) {
        try {
            HttpResponse<String> response = requestDetailWithRetry(detailUrl.trim());
            if (isDeferredResponse(response.statusCode())) {
                log.warn("CourtListener detail deferred by anti-bot response status={}", response.statusCode());
                return "";
            }
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }
            String html = response.body();
            Document doc = Jsoup.parse(html, detailUrl);
            doc.select("script,style,noscript,header,footer,nav,form").remove();

            String title = str(doc.title());
            Element main = firstPresent(doc,
                    ".opinion-content",
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
        } catch (Exception e) {
            log.warn("HTML detail fetch failed url={} : {}", detailUrl, e.getMessage());
            return "";
        }
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
