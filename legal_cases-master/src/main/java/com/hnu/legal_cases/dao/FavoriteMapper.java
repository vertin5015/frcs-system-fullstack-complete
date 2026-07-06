package com.hnu.legal_cases.dao;

import com.hnu.legal_cases.pojo.UserFavorites;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FavoriteMapper {
    /**
     * 添加收藏
     */
    void addFavorite(@Param("userId") Long userId, @Param("caseId") String caseId);

    /**
     * 删除收藏
     */
    void delFavorite(@Param("userId") Long userId, @Param("caseId") String caseId);

    /**
     * 根据caseId查询收藏记录
     */
    UserFavorites getFavoriteCase(@Param("userId") Long userId, @Param("caseId") String caseId);

    /**
     * 收藏案例列表
     */
    List<UserFavorites> queryFavoriteCases(@Param("userId") Long userId);

    /**
     * 查看案例被收藏了多少次
     */
    Integer getFavoritedCount(String caseId);
}
