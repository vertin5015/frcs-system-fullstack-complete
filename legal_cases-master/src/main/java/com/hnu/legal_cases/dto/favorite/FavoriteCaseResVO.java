package com.hnu.legal_cases.dto.favorite;

import lombok.Data;

import java.util.List;

/**
 * 收藏案例列表返回结果
 */
@Data
public class FavoriteCaseResVO {
    /**
     * 收藏案例总数
     */
    private Integer totalCount;
    /**
     * 收藏案例列表
     */
    private List<FavoriteInfo> favoriteInfoList;
}
