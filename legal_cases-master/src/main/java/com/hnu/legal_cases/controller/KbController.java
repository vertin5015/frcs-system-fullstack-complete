package com.hnu.legal_cases.controller;

import com.hnu.legal_cases.dto.kb.KbIngestReqVO;
import com.hnu.legal_cases.dto.kb.KbIngestCrawlerReqVO;
import com.hnu.legal_cases.dto.kb.KbIngestDbReqVO;
import com.hnu.legal_cases.dto.kb.KbQueryReqVO;
import com.hnu.legal_cases.dto.kb.KbQueryResVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.LocalKbService;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/kb")
@RequiredArgsConstructor
public class KbController {

    private final LocalKbService localKbService;

    @PostMapping("/ingest")
    public JSONReturnBean<String> ingest(@RequestBody KbIngestReqVO reqVO) {
        try {
            int count = localKbService.ingest(reqVO);
            return JSONReturnBean.success("入库成功，新增切片数: " + count);
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("kb ingest", e);
            return JSONReturnBean.failed("知识库入库失败");
        }
    }

    @PostMapping("/ingest-crawler")
    public JSONReturnBean<String> ingestCrawler(@RequestBody KbIngestCrawlerReqVO reqVO) {
        try {
            int count = localKbService.ingestFromCrawler(reqVO);
            return JSONReturnBean.success("爬取源入库成功，新增切片数: " + count);
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("kb ingest-crawler", e);
            return JSONReturnBean.failed("爬取源入库失败");
        }
    }

    @PostMapping("/ingest-db")
    public JSONReturnBean<String> ingestDb(@RequestBody(required = false) KbIngestDbReqVO reqVO) {
        try {
            int count = localKbService.ingestFromDb(reqVO);
            return JSONReturnBean.success("数据库入库成功，新增切片数: " + count);
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("kb ingest-db", e);
            return JSONReturnBean.failed("数据库入库失败");
        }
    }

    @PostMapping("/query")
    public JSONReturnBean<KbQueryResVO> query(@RequestBody KbQueryReqVO reqVO) {
        try {
            return JSONReturnBean.success(localKbService.query(reqVO));
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("kb query", e);
            return JSONReturnBean.failed("知识库查询失败");
        }
    }
}
