package com.hnu.legal_cases.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnu.legal_cases.config.KbProperties;
import com.hnu.legal_cases.dao.CaseDetailMapper;
import com.hnu.legal_cases.dto.ai.SpringAIResVO;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.dto.kb.KbIngestCrawlerReqVO;
import com.hnu.legal_cases.dto.kb.KbIngestDbReqVO;
import com.hnu.legal_cases.dto.kb.KbIngestReqVO;
import com.hnu.legal_cases.dto.kb.KbQueryReqVO;
import com.hnu.legal_cases.dto.kb.KbQueryResVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.CaseDetailInfo;
import com.hnu.legal_cases.service.LocalKbService;
import com.hnu.legal_cases.service.LocalEmbeddingService;
import com.hnu.legal_cases.service.CrawlerService;
import com.hnu.legal_cases.service.SpringAIService;
import io.micrometer.common.util.StringUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalKbServiceImpl implements LocalKbService {

    private final LocalEmbeddingService localEmbeddingService;
    private final CaseDetailMapper caseDetailMapper;
    private final CrawlerService crawlerService;
    private final SpringAIService springAIService;
    private final ObjectMapper objectMapper;
    private final KbProperties kbProperties;
    @Qualifier("summaryClient")
    private final ChatClient summaryClient;

    @Override
    public synchronized int ingest(KbIngestReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        String sourceId = StringUtils.isNotBlank(reqVO.getSourceId()) ? reqVO.getSourceId().trim() : "manual";
        String title = StringUtils.isNotBlank(reqVO.getTitle()) ? reqVO.getTitle().trim() : "";
        String content = resolveContent(reqVO);
        if (StringUtils.isBlank(content)) {
            throw new ServiceException("知识库入库内容为空");
        }

        List<String> chunks = splitIntoChunks(content.trim(), kbProperties.getChunkSize(), kbProperties.getChunkOverlap());
        if (chunks.isEmpty()) {
            throw new ServiceException("切分后没有可入库内容");
        }

        List<KbChunk> all = loadAll();
        int inserted = appendChunks(all, sourceId, title, chunks);
        saveAll(all);
        log.info("本地知识库入库完成 sourceId={} chunks={}", sourceId, inserted);
        return inserted;
    }

    @Override
    public synchronized int ingestFromCrawler(KbIngestCrawlerReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getKeyword())) {
            throw new ServiceException("关键词为空");
        }
        String country = StringUtils.isBlank(reqVO.getCountry()) ? null : reqVO.getCountry().trim();
        String keyword = reqVO.getKeyword().trim();
        String extracted = springAIService.extractKeyword(keyword, country);
        List<CrawlerBaseInfoItem> items = crawlerService.queryCaseBaseInfo(extracted, country, reqVO.getPeriod(), reqVO.getSources());
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int limit = reqVO.getLimit() == null ? 50 : Math.max(1, Math.min(500, reqVO.getLimit()));
        List<KbChunk> all = loadAll();
        int inserted = 0;
        int cnt = 0;
        for (CrawlerBaseInfoItem item : items) {
            if (cnt >= limit) {
                break;
            }
            String content = buildCrawlerDocument(item);
            if (StringUtils.isBlank(content)) {
                continue;
            }
            String sourceId = StringUtils.isNotBlank(item.getDocketNumber()) ? item.getDocketNumber() : ("crawler_" + cnt);
            String title = item.getTitle();
            List<String> chunks = splitIntoChunks(content, kbProperties.getChunkSize(), kbProperties.getChunkOverlap());
            inserted += appendChunks(all, sourceId, title, chunks);
            cnt++;
        }
        saveAll(all);
        log.info("爬取源入库完成 keyword={} country={} insertedChunks={}", keyword, country, inserted);
        return inserted;
    }

    @Override
    public synchronized int ingestCrawlerItems(List<CrawlerBaseInfoItem> items, int limit) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int max = Math.max(1, Math.min(500, limit));
        List<KbChunk> all = loadAll();
        int inserted = 0;
        int cnt = 0;
        for (CrawlerBaseInfoItem item : items) {
            if (cnt >= max) {
                break;
            }
            String content = buildCrawlerDocument(item);
            if (StringUtils.isBlank(content)) {
                continue;
            }
            String sourceId = StringUtils.isNotBlank(item.getDocketNumber())
                    ? "case:" + item.getDocketNumber().trim()
                    : "crawler:" + UUID.randomUUID().toString().substring(0, 8);
            removeExistingSourceChunks(all, sourceId);
            List<String> chunks = splitIntoChunks(content, kbProperties.getChunkSize(), kbProperties.getChunkOverlap());
            inserted += appendChunks(all, sourceId, item.getTitle(), chunks);
            cnt++;
        }
        if (inserted > 0) {
            saveAll(all);
        }
        log.info("搜索结果后台预热知识库完成 items={} insertedChunks={}", cnt, inserted);
        return inserted;
    }

    @Override
    public synchronized int ingestCaseDocument(String sourceId, String title, String content) {
        if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(content)) {
            return 0;
        }
        String normalizedSourceId = "case-full:" + sourceId.trim();
        String normalizedTitle = StringUtils.isBlank(title) ? "case_full_text" : title.trim();
        List<String> chunks = splitIntoChunks(content.trim(), kbProperties.getChunkSize(), kbProperties.getChunkOverlap());
        if (chunks.isEmpty()) {
            return 0;
        }
        List<KbChunk> all = loadAll();
        removeExistingSourceChunks(all, normalizedSourceId);
        int inserted = appendChunks(all, normalizedSourceId, normalizedTitle, chunks);
        saveAll(all);
        log.info("完整原文入库完成 sourceId={} chunks={}", normalizedSourceId, inserted);
        return inserted;
    }

    @Override
    public synchronized int ingestFromDb(KbIngestDbReqVO reqVO) {
        int limit = reqVO == null || reqVO.getLimit() == null ? 100 : Math.max(1, Math.min(2000, reqVO.getLimit()));
        String lang = (reqVO == null || StringUtils.isBlank(reqVO.getLanguage())) ? "zh" : reqVO.getLanguage().trim().toLowerCase(Locale.ROOT);
        List<CaseDetailInfo> rows = caseDetailMapper.listRecentForKb(limit);
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        List<KbChunk> all = loadAll();
        int inserted = 0;
        for (CaseDetailInfo row : rows) {
            String content = "en".equals(lang) ? row.getContentEnUs() : row.getContentZhCn();
            if (StringUtils.isBlank(content)) {
                content = StringUtils.isNotBlank(row.getContentZhCn()) ? row.getContentZhCn() : row.getContentEnUs();
            }
            if (StringUtils.isBlank(content)) {
                continue;
            }
            List<String> chunks = splitIntoChunks(content.trim(), kbProperties.getChunkSize(), kbProperties.getChunkOverlap());
            inserted += appendChunks(all, row.getCaseId(), "case_detail_info", chunks);
        }
        saveAll(all);
        log.info("数据库入库完成 limit={} insertedChunks={}", limit, inserted);
        return inserted;
    }

    @Override
    public synchronized KbQueryResVO query(KbQueryReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getQuestion())) {
            throw new ServiceException("问题为空");
        }
        List<KbChunk> all = loadAll();
        if (all.isEmpty()) {
            KbQueryResVO empty = new KbQueryResVO();
            empty.setAnswer("本地知识库为空，请先入库。");
            empty.setHitCount(0);
            empty.setHits(List.of());
            return empty;
        }

        float[] qv = localEmbeddingService.embed(reqVO.getQuestion().trim());
        int topK = normalizedTopK(reqVO.getTopK(), all.size());
        final String qText = reqVO.getQuestion().trim();
        final float[] queryVector = qv;
        List<ScoredChunk> ranked = all.stream()
                .map(c -> new ScoredChunk(c, similarityScore(c, queryVector, qText)))
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(topK)
                .toList();

        String lang = StringUtils.isNotBlank(reqVO.getLanguage()) ? reqVO.getLanguage().trim() : "zh";
        String context = buildContext(ranked);
        String answer;
        try {
            answer = askOpenAi(reqVO.getQuestion().trim(), context, lang);
        } catch (Exception e) {
            log.warn("kb query 调用 OpenAI 失败，返回检索片段兜底: {}", e.getMessage());
            answer = buildFallbackAnswer(ranked, lang);
        }

        KbQueryResVO res = new KbQueryResVO();
        res.setAnswer(answer);
        res.setHitCount(ranked.size());
        List<KbQueryResVO.Hit> hits = new ArrayList<>();
        for (ScoredChunk r : ranked) {
            KbQueryResVO.Hit h = new KbQueryResVO.Hit();
            h.setChunkId(r.getChunk().getChunkId());
            h.setSourceId(r.getChunk().getSourceId());
            h.setTitle(r.getChunk().getTitle());
            h.setScore(r.getScore());
            h.setPreview(preview(r.getChunk().getContent(), 120));
            hits.add(h);
        }
        res.setHits(hits);
        return res;
    }

    private String askOpenAi(String question, String context, String language) {
        String outLang = language.toLowerCase(Locale.ROOT).startsWith("zh") ? "中文" : "English";
        String prompt = """
                你是法律知识库问答助手。请基于给定的知识片段回答问题，不要编造事实。
                如果知识片段无法支持答案，请明确回复“知识库中暂无足够信息”。
                回答语言：%s
                
                【问题】
                %s
                
                【知识片段】
                %s
                
                输出 JSON：
                {"status":"ok","result":"你的回答"}
                """.formatted(outLang, question, context);
        SpringAIResVO vo = summaryClient.prompt().user(prompt).call().entity(SpringAIResVO.class);
        if (vo != null && "ok".equals(vo.getStatus()) && StringUtils.isNotBlank(vo.getResult())) {
            return vo.getResult().trim();
        }
        throw new ServiceException("知识库问答失败，请稍后再试");
    }

    private static String buildFallbackAnswer(List<ScoredChunk> ranked, String language) {
        boolean zh = language != null && language.toLowerCase(Locale.ROOT).startsWith("zh");
        if (ranked == null || ranked.isEmpty()) {
            return zh ? "未检索到相关知识片段。" : "No relevant knowledge snippets were retrieved.";
        }
        StringBuilder sb = new StringBuilder();
        if (zh) {
            sb.append("OpenAI 暂不可用，以下返回知识库命中片段（按相关度排序）：\n\n");
        } else {
            sb.append("OpenAI chat is temporarily unavailable. Retrieved knowledge snippets (ranked):\n\n");
        }
        int i = 1;
        for (ScoredChunk r : ranked) {
            KbChunk c = r.getChunk();
            String title = StringUtils.isNotBlank(c.getTitle()) ? normalizeForDisplay(c.getTitle()) : "-";
            String snippet = preview(normalizeForDisplay(c.getContent()), 280);
            sb.append(i++)
                    .append(". [source=")
                    .append(c.getSourceId())
                    .append("] [title=")
                    .append(title)
                    .append("] [score=")
                    .append(String.format("%.4f", r.getScore()))
                    .append("]\n");
            sb.append(snippet).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static String normalizeForDisplay(String text) {
        if (text == null) {
            return "";
        }
        // 去掉控制字符并压缩空白，避免终端/前端展示杂乱
        String s = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
        s = s.replace('\uFFFD', ' ');
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private int appendChunks(List<KbChunk> all, String sourceId, String title, List<String> chunks) {
        int inserted = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            KbChunk c = new KbChunk();
            c.setChunkId(sourceId + "#" + (i + 1) + "#" + UUID.randomUUID().toString().substring(0, 8));
            c.setSourceId(sourceId);
            c.setTitle(title);
            c.setContent(chunkText);
            c.setEmbedding(embedSafely(chunkText));
            c.setCreatedAt(LocalDateTime.now().toString());
            all.add(c);
            inserted++;
        }
        return inserted;
    }

    private void removeExistingSourceChunks(List<KbChunk> all, String sourceId) {
        if (StringUtils.isBlank(sourceId)) {
            return;
        }
        all.removeIf(c -> sourceId.equals(c.getSourceId()));
    }

    private List<Float> embedSafely(String text) {
        return toList(localEmbeddingService.embed(text));
    }

    private static double similarityScore(KbChunk c, float[] qv, String question) {
        if (qv != null && c.getEmbedding() != null && !c.getEmbedding().isEmpty()) {
            return cosine(toArray(c.getEmbedding()), qv);
        }
        return keywordOverlapScore(question, c.getContent());
    }

    private static double keywordOverlapScore(String question, String content) {
        if (StringUtils.isBlank(question) || StringUtils.isBlank(content)) {
            return 0.0;
        }
        String q = question.toLowerCase(Locale.ROOT);
        String c = content.toLowerCase(Locale.ROOT);
        String[] terms = q.split("\\s+");
        int hit = 0;
        int total = 0;
        for (String t : terms) {
            if (t == null || t.isBlank() || t.length() < 2) {
                continue;
            }
            total++;
            if (c.contains(t)) {
                hit++;
            }
        }
        if (total == 0) {
            return c.contains(q) ? 1.0 : 0.0;
        }
        return (double) hit / (double) total;
    }

    private static String buildCrawlerDocument(CrawlerBaseInfoItem item) {
        if (item == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(item.getTitle())) {
            sb.append("Title: ").append(item.getTitle().trim()).append("\n");
        }
        if (StringUtils.isNotBlank(item.getDocketNumber())) {
            sb.append("CaseId: ").append(item.getDocketNumber().trim()).append("\n");
        }
        if (StringUtils.isNotBlank(item.getDateFiled())) {
            sb.append("JudgementDate: ").append(item.getDateFiled().trim()).append("\n");
        }
        if (StringUtils.isNotBlank(item.getUrl())) {
            sb.append("URL: ").append(item.getUrl().trim()).append("\n");
        }
        if (StringUtils.isNotBlank(item.getSummary())) {
            sb.append("\nSummary:\n").append(item.getSummary().trim());
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private String resolveContent(KbIngestReqVO reqVO) {
        if (StringUtils.isNotBlank(reqVO.getContent())) {
            return reqVO.getContent();
        }
        if (StringUtils.isBlank(reqVO.getSourceId())) {
            return null;
        }
        CaseDetailInfo info = caseDetailMapper.getCaseDetailByCaseId(reqVO.getSourceId().trim());
        if (info == null) {
            return null;
        }
        if (StringUtils.isNotBlank(info.getContentZhCn())) {
            return info.getContentZhCn();
        }
        return info.getContentEnUs();
    }

    private int normalizedTopK(Integer topK, int dataSize) {
        int v = topK == null ? kbProperties.getDefaultTopK() : topK;
        int max = kbProperties.getMaxTopK();
        if (v <= 0) {
            v = kbProperties.getDefaultTopK();
        }
        if (v > max) {
            v = max;
        }
        return Math.min(v, dataSize);
    }

    private String buildContext(List<ScoredChunk> ranked) {
        StringBuilder sb = new StringBuilder();
        for (ScoredChunk r : ranked) {
            KbChunk c = r.getChunk();
            sb.append("- [")
                    .append(c.getChunkId())
                    .append("] source=")
                    .append(c.getSourceId())
                    .append(", score=")
                    .append(String.format("%.4f", r.getScore()))
                    .append("\n")
                    .append(c.getContent())
                    .append("\n\n");
        }
        return sb.toString();
    }

    private List<KbChunk> loadAll() {
        Path p = indexPath();
        if (!Files.exists(p)) {
            return new ArrayList<>();
        }
        try {
            byte[] raw = Files.readAllBytes(p);
            if (raw.length == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(raw, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new ServiceException("读取本地知识库失败: " + e.getMessage());
        }
    }

    private void saveAll(List<KbChunk> all) {
        Path p = indexPath();
        try {
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            byte[] raw = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(all);
            Files.write(p, raw, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new ServiceException("保存本地知识库失败: " + e.getMessage());
        }
    }

    private Path indexPath() {
        return Path.of(kbProperties.getIndexFile());
    }

    private static List<Float> toList(float[] v) {
        List<Float> out = new ArrayList<>(v.length);
        for (float f : v) {
            out.add(f);
        }
        return out;
    }

    private static float[] toArray(List<Float> list) {
        float[] out = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    private static double cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        if (n == 0) {
            return 0.0;
        }
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static String preview(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "...";
    }

    private static List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> out = new ArrayList<>();
        if (StringUtils.isBlank(text)) {
            return out;
        }
        int size = Math.max(200, chunkSize);
        int ov = Math.max(0, Math.min(overlap, size / 2));
        int step = size - ov;
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + size, text.length());
            String c = text.substring(i, end).trim();
            if (StringUtils.isNotBlank(c)) {
                out.add(c);
            }
            if (end >= text.length()) {
                break;
            }
        }
        return out;
    }

    @Data
    private static class KbChunk {
        private String chunkId;
        private String sourceId;
        private String title;
        private String content;
        private List<Float> embedding;
        private String createdAt;
    }

    @Data
    @RequiredArgsConstructor
    private static class ScoredChunk {
        private final KbChunk chunk;
        private final Double score;
    }
}
