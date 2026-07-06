package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.ai.SummaryCaseReqVO;
import com.hnu.legal_cases.dto.browse.BrowseHistoryCaseReqVO;
import com.hnu.legal_cases.dto.cases.SearchCasesReqVO;
import com.hnu.legal_cases.dto.favorite.FavoriteCaseReqVO;
import com.hnu.legal_cases.dto.favorite.QueryFavoriteCaseReqVO;
import com.hnu.legal_cases.dto.user.LoginReqVO;
import com.hnu.legal_cases.dto.ai.CaseQaReqVO;
import com.hnu.legal_cases.dto.user.RegisterReqVO;

public interface CheckReqVOService {
    /**
     * 校验搜索案例入参
     *
     * @param reqVO 搜索案例入参
     */
    void checkSearchCasesReqVO(SearchCasesReqVO reqVO);

    /**
     * 校验ai总结案例入参
     *
     * @param reqVO ai总结案例入参
     */
    void checkSummaryCaseReqVO(SummaryCaseReqVO reqVO);

    /**
     * 校验用户注册入参
     *
     * @param reqVO 用户注册入参
     */
    void checkRegisterReqVO(RegisterReqVO reqVO);

    /**
     * 校验用户登录入参
     *
     * @param reqVO 用户登录入参
     */
    void checkLoginReqVO(LoginReqVO reqVO);

    /**
     * 校验收藏案例入参
     *
     * @param reqVO 收藏案例入参
     */
    void checkFavoriteCaseVO(FavoriteCaseReqVO reqVO);

    /**
     * 校验用户收藏案例列表入参
     *
     * @param reqVO 用户收藏案例列表入参
     */
    void checkQueryFavoriteCaseVO(QueryFavoriteCaseReqVO reqVO);

    /**
     * 校验用户浏览历史入参
     *
     * @param reqVO 用户浏览历史入参
     */
    void checkBrowseHistoryCaseReqVO(BrowseHistoryCaseReqVO reqVO);

    void checkCaseQaReqVO(CaseQaReqVO reqVO);
}
