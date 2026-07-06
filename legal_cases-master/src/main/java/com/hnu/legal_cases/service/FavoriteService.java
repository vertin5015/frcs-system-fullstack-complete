package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.favorite.FavoriteCaseReqVO;
import com.hnu.legal_cases.dto.favorite.FavoriteCaseResVO;
import com.hnu.legal_cases.dto.favorite.QueryFavoriteCaseReqVO;

public interface FavoriteService {
    void addFavorite(FavoriteCaseReqVO reqVO);

    void delFavorite(FavoriteCaseReqVO reqVO);

    Boolean isFavorite(Long userId, String caseId);

    FavoriteCaseResVO queryFavoriteCases(QueryFavoriteCaseReqVO reqVO);

    Integer getFavoritedCount(String caseId);
}
