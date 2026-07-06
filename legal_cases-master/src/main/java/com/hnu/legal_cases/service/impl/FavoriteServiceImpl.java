package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.CaseMapper;
import com.hnu.legal_cases.dao.FavoriteMapper;
import com.hnu.legal_cases.dto.favorite.FavoriteCaseReqVO;
import com.hnu.legal_cases.dto.favorite.FavoriteCaseResVO;
import com.hnu.legal_cases.dto.favorite.FavoriteInfo;
import com.hnu.legal_cases.dto.favorite.QueryFavoriteCaseReqVO;
import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.enums.PeriodEnum;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.CaseInfo;
import com.hnu.legal_cases.pojo.UserFavorites;
import com.hnu.legal_cases.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private CaseMapper caseMapper;

    /**
     * 添加收藏
     */
    @Override
    public void addFavorite(FavoriteCaseReqVO reqVO) {
        Long userId = reqVO.getUserId();
        String caseId = reqVO.getCaseId();

        UserFavorites userFavorites = favoriteMapper.getFavoriteCase(userId, caseId);
        if (userFavorites != null) {
            throw new ServiceException("该案例已收藏");
        }

        favoriteMapper.addFavorite(userId, caseId);
    }

    /**
     * 删除收藏
     */
    @Override
    public void delFavorite(FavoriteCaseReqVO reqVO) {
        Long userId = reqVO.getUserId();
        String caseId = reqVO.getCaseId();

        UserFavorites userFavorites = favoriteMapper.getFavoriteCase(userId, caseId);
        if (userFavorites == null) {
            throw new ServiceException("该案例未收藏");
        }

        favoriteMapper.delFavorite(userId, caseId);
    }

    @Override
    public Boolean isFavorite(Long userId, String caseId) {
        UserFavorites userFavorites = favoriteMapper.getFavoriteCase(userId, caseId);
        return userFavorites != null;
    }

    /*
     * 获取收藏列表，按收藏日期排序
     */
    @Override
    public FavoriteCaseResVO queryFavoriteCases(QueryFavoriteCaseReqVO reqVO) {
        Long userId = reqVO.getUserId();
        String language = reqVO.getLanguage();
        String startDate = PeriodEnum.getStartDateByYear(reqVO.getPeriod());
        Integer sourceId = CountryEnum.getSourceIdByCode(reqVO.getCountry());
        int startIndex = reqVO.getStartIndex();
        int endIndex = reqVO.getEndIndex();

        FavoriteCaseResVO resVO = new FavoriteCaseResVO();

        // 查询该用户所有的收藏案例
        List<UserFavorites> userFavoritesList = favoriteMapper.queryFavoriteCases(userId);
        if (userFavoritesList == null || userFavoritesList.isEmpty()) {
            resVO.setTotalCount(0);
            resVO.setFavoriteInfoList(new ArrayList<>());
            return resVO;
        }

        // 筛选出所有符合条件的收藏案例
        List<FavoriteInfo> favoriteInfoList = this.filterFavoriteCases(userFavoritesList, sourceId, startDate, language);

        // 组装结果
        resVO.setTotalCount(favoriteInfoList.size());
        if (favoriteInfoList.size() <= startIndex) {
            resVO.setFavoriteInfoList(new ArrayList<>());
            return resVO;
        }
        resVO.setFavoriteInfoList(favoriteInfoList.subList(startIndex, Math.min(endIndex, favoriteInfoList.size())));
        return resVO;
    }

    @Override
    public Integer getFavoritedCount(String caseId) {
        return favoriteMapper.getFavoritedCount(caseId);
    }

    /**
     * 筛选出所有符合条件的收藏案例
     * 条件：国家、判决时间
     */
    private List<FavoriteInfo> filterFavoriteCases(List<UserFavorites> userFavoritesList, Integer sourceId, String startDate, String language) {
        Set<String> caseIdList = userFavoritesList.stream().map(UserFavorites::getCaseId).collect(Collectors.toSet());

        // 获取筛选后结果
        List<CaseInfo> caseInfoList = caseMapper.queryCasesBySourceIdAndDate(caseIdList, language, sourceId, startDate);
        if (caseInfoList == null || caseInfoList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, CaseInfo> caseInfoMap = caseInfoList.stream()
                .collect(Collectors.toMap(CaseInfo::getCaseId, Function.identity()));

        // 组装FavoriteInfo列表
        List<FavoriteInfo> favoriteInfoList = new ArrayList<>();
        for (UserFavorites userFavorite : userFavoritesList) {
            String caseId = userFavorite.getCaseId();
            CaseInfo caseInfo = caseInfoMap.get(caseId);

            // 只有当案例信息存在时才添加到结果列表
            if (caseInfo != null) {
                FavoriteInfo favoriteInfo = new FavoriteInfo();
                favoriteInfo.setCaseId(caseId);
                favoriteInfo.setCaseName(caseInfo.getCaseName());
                favoriteInfo.setJudgementDate(caseInfo.getJudgmentDate());
                favoriteInfo.setCountry(CountryEnum.getCodeBySourceId(caseInfo.getSourceId()));
                favoriteInfo.setTags(caseInfo.getSummary());
                favoriteInfo.setCitationCount(caseInfo.getCitationCount());
                favoriteInfo.setOriginalDocumentUrl(caseInfo.getOriginalDocumentUrl());
                favoriteInfo.setFavoriteDate(userFavorite.getFavoriteDate());

                favoriteInfoList.add(favoriteInfo);
            }
        }

        // 按收藏时间倒序排列
        favoriteInfoList.sort((f1, f2) -> f2.getFavoriteDate().compareTo(f1.getFavoriteDate()));
        return favoriteInfoList;
    }
}
