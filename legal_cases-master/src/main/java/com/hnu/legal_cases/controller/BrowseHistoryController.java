package com.hnu.legal_cases.controller;

import com.alibaba.fastjson.JSON;
import com.hnu.legal_cases.dto.browse.BrowseHistoryCaseReqVO;
import com.hnu.legal_cases.dto.browse.BrowseHistoryCaseResVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.BrowseHistoryService;
import com.hnu.legal_cases.service.CheckReqVOService;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 浏览记录
 *
 * @author cyx
 * @date 2025-09-08
 */
@Slf4j
@RestController
@RequestMapping("/history")
public class BrowseHistoryController {
    @Autowired
    BrowseHistoryService browseHistoryService;
    @Autowired
    CheckReqVOService checkReqVOService;

    @GetMapping("/browse_history")
    public JSONReturnBean<BrowseHistoryCaseResVO> browseHistoryList(@ModelAttribute BrowseHistoryCaseReqVO reqVO){
        try {
            checkReqVOService.checkBrowseHistoryCaseReqVO(reqVO);
            BrowseHistoryCaseResVO resVO = browseHistoryService.browseHistoryList(reqVO);
            return JSONReturnBean.success(resVO);
        } catch (ServiceException e) {
            log.error("查看浏览记录异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("查看浏览记录错误，请稍后再试，error=", e);
            return JSONReturnBean.failed("查看浏览记录错误，请稍后再试");
        }
    }
}
