package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.kb.KbIngestReqVO;
import com.hnu.legal_cases.dto.kb.KbIngestCrawlerReqVO;
import com.hnu.legal_cases.dto.kb.KbIngestDbReqVO;
import com.hnu.legal_cases.dto.kb.KbQueryReqVO;
import com.hnu.legal_cases.dto.kb.KbQueryResVO;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;

import java.util.List;

public interface LocalKbService {
    int ingest(KbIngestReqVO reqVO);

    int ingestFromCrawler(KbIngestCrawlerReqVO reqVO);

    int ingestFromDb(KbIngestDbReqVO reqVO);

    int ingestCrawlerItems(List<CrawlerBaseInfoItem> items, int limit);

    int ingestCaseDocument(String sourceId, String title, String content);

    KbQueryResVO query(KbQueryReqVO reqVO);
}
