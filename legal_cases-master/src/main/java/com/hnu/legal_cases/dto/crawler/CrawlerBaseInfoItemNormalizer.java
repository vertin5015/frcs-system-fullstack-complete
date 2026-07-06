package com.hnu.legal_cases.dto.crawler;

import cn.hutool.crypto.SecureUtil;
import io.micrometer.common.util.StringUtils;

/**
 * 爬虫条目案号补全：各数据源 JSON 字段不一致时可能缺少「Docket Number」，
 * 导致无法去重、无法写 Redis、无法落库。对无案号但有 url/title 的条目生成稳定占位案号。
 */
public final class CrawlerBaseInfoItemNormalizer {

    private CrawlerBaseInfoItemNormalizer() {
    }

    /**
     * 已有非空案号则 trim；否则用 url 或 title 派生 GEN&lt;sourceId&gt;_&lt;hash&gt;，与同一 url 多次合并结果一致。
     */
    public static void ensureStableDocketNumber(CrawlerBaseInfoItem item) {
        if (item == null) {
            return;
        }
        String raw = item.getDocketNumber();
        if (StringUtils.isNotBlank(raw)) {
            item.setDocketNumber(raw.trim());
            return;
        }
        int sid = item.getSourceId() != null ? item.getSourceId() : 0;
        if (StringUtils.isNotBlank(item.getUrl())) {
            String h = SecureUtil.md5(item.getUrl().trim());
            item.setDocketNumber("GEN" + sid + "_" + h.substring(0, 24));
            return;
        }
        if (StringUtils.isNotBlank(item.getTitle())) {
            String h = SecureUtil.md5(item.getTitle().trim() + ":" + sid);
            item.setDocketNumber("GEN" + sid + "_" + h.substring(0, 24));
        }
    }
}
