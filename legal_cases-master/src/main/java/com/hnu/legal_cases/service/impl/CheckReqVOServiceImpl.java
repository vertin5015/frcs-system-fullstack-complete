package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dto.ai.CaseQaReqVO;
import com.hnu.legal_cases.dto.ai.SummaryCaseReqVO;
import com.hnu.legal_cases.dto.browse.BrowseHistoryCaseReqVO;
import com.hnu.legal_cases.dto.cases.SearchCasesReqVO;
import com.hnu.legal_cases.dto.favorite.FavoriteCaseReqVO;
import com.hnu.legal_cases.dto.favorite.QueryFavoriteCaseReqVO;
import com.hnu.legal_cases.dto.user.LoginReqVO;
import com.hnu.legal_cases.dto.user.RegisterReqVO;
import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.enums.LanguageEnum;
import com.hnu.legal_cases.enums.PeriodEnum;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.CheckReqVOService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CheckReqVOServiceImpl implements CheckReqVOService {
    // 游客用户id
    private static final Long GUEST_USER_ID = 0L;

    /**
     * 校验搜索案例入参
     *
     * @param reqVO 搜索案例入参
     */
    @Override
    public void checkSearchCasesReqVO(SearchCasesReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        if (reqVO.getUserId() == null) {
            throw new ServiceException("用户id为空");
        }
        if (StringUtils.isBlank(reqVO.getKeyword())) {
            throw new ServiceException("关键词为空");
        }
        if (!LanguageEnum.checkValidCode(reqVO.getLanguage())) {
            throw new ServiceException("无效的语言");
        }
        if (StringUtils.isNotBlank(reqVO.getCountry()) && !CountryEnum.checkValidCode(reqVO.getCountry())) {
            throw new ServiceException("无效的国家");
        }
        if (reqVO.getPeriod() != null && !PeriodEnum.checkValidCode(reqVO.getPeriod())) {
            throw new ServiceException("无效的判决时间");
        }
        if (StringUtils.isNotBlank(reqVO.getSources())) {
            for (String part : reqVO.getSources().split(",")) {
                String code = part == null ? "" : part.trim();
                if (code.isEmpty()) {
                    continue;
                }
                if (!CountryEnum.checkValidCode(code)) {
                    throw new ServiceException("无效的数据源代码: " + code);
                }
            }
        }
    }

    /**
     * 校验ai总结案例入参
     *
     * @param reqVO ai总结案例入参
     */
    @Override
    public void checkSummaryCaseReqVO(SummaryCaseReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        if (reqVO.getUserId() == null) {
            throw new ServiceException("用户id为空");
        }
        if (StringUtils.isBlank(reqVO.getCaseId())) {
            throw new ServiceException("caseId为空");
        }
        if (GUEST_USER_ID.equals(reqVO.getUserId())) {
            throw new ServiceException("请先登录后使用 AI 摘要功能");
        }
        if (!LanguageEnum.checkValidCode(reqVO.getLanguage())) {
            throw new ServiceException("无效的语言");
        }
    }

    /**
     * 校验用户注册入参
     *
     * @param reqVO 用户注册入参
     */
    @Override
    public void checkRegisterReqVO(RegisterReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        if (StringUtils.isBlank(reqVO.getUsername())) {
            throw new ServiceException("用户名为空");
        }
        if (StringUtils.isBlank(reqVO.getEmail())) {
            throw new ServiceException("邮箱为空");
        }
        if (StringUtils.isBlank(reqVO.getPassword())) {
            throw new ServiceException("密码为空");
        }
    }

    /**
     * 校验用户登录入参
     *
     * @param reqVO 用户登录入参
     */
    @Override
    public void checkLoginReqVO(LoginReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        if (StringUtils.isBlank(reqVO.getEmail())) {
            throw new ServiceException("邮箱为空");
        }
        if (StringUtils.isBlank(reqVO.getPassword())) {
            throw new ServiceException("密码为空");
        }
    }

    /**
     * 校验用户收藏案例入参
     *
     * @param reqVO 用户收藏案例入参
     */
    @Override
    public void checkFavoriteCaseVO(FavoriteCaseReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        if (reqVO.getUserId() == null) {
            throw new ServiceException("用户id为空");
        }
        if (reqVO.getUserId().equals(GUEST_USER_ID)){
            throw new ServiceException("游客用户禁止使用收藏功能，请先注册账号");
        }
        if (StringUtils.isBlank(reqVO.getCaseId())) {
            throw new ServiceException("案例id为空");
        }
        if (reqVO.getUserId() == null) {
            throw new ServiceException("用户id为空");
        }
    }

    /**
     * 校验查询用户收藏案例列表入参
     *
     * @param reqVO 查询用户收藏案例列表入参
     */
    @Override
    public void checkQueryFavoriteCaseVO(QueryFavoriteCaseReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        if (reqVO.getUserId() == null) {
            throw new ServiceException("用户id为空");
        }
        if (reqVO.getUserId().equals(GUEST_USER_ID)){
            throw new ServiceException("游客用户禁止使用收藏功能，请先注册账号");
        }
        if (!LanguageEnum.checkValidCode(reqVO.getLanguage())) {
            throw new ServiceException("无效的语言");
        }
        if (StringUtils.isNotBlank(reqVO.getCountry()) && !CountryEnum.checkValidCode(reqVO.getCountry())) {
            throw new ServiceException("无效的国家");
        }
        if (reqVO.getPeriod() != null && !PeriodEnum.checkValidCode(reqVO.getPeriod())) {
            throw new ServiceException("无效的判决时间");
        }
    }

    /**
     * 校验用户浏览历史入参
     *
     * @param reqVO 用户浏览历史入参
     */
    @Override
    public void checkBrowseHistoryCaseReqVO(BrowseHistoryCaseReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        if (reqVO.getUserId() == null) {
            throw new ServiceException("用户id为空");
        }
        if (reqVO.getUserId().equals(GUEST_USER_ID)){
            throw new ServiceException("游客用户禁止使用浏览历史功能，请先注册账号");
        }
        if (!LanguageEnum.checkValidCode(reqVO.getLanguage())) {
            throw new ServiceException("无效的语言");
        }
        if (StringUtils.isNotBlank(reqVO.getCountry()) && !CountryEnum.checkValidCode(reqVO.getCountry())) {
            throw new ServiceException("无效的国家");
        }
        if (reqVO.getPeriod() != null && !PeriodEnum.checkValidCode(reqVO.getPeriod())) {
            throw new ServiceException("无效的判决时间");
        }
    }

    @Override
    public void checkCaseQaReqVO(CaseQaReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("入参为空");
        }
        if (reqVO.getUserId() == null) {
            throw new ServiceException("用户id为空");
        }
        if (GUEST_USER_ID.equals(reqVO.getUserId())) {
            throw new ServiceException("请先登录后使用本案问答");
        }
        if (StringUtils.isBlank(reqVO.getCaseId())) {
            throw new ServiceException("caseId为空");
        }
        if (StringUtils.isBlank(reqVO.getQuestion())) {
            throw new ServiceException("问题为空");
        }
        if (!LanguageEnum.checkValidCode(reqVO.getLanguage())) {
            throw new ServiceException("无效的语言");
        }
    }

}
