package com.hnu.legal_cases.controller;

import com.alibaba.fastjson.JSON;
import com.hnu.legal_cases.dto.favorite.FavoriteCaseReqVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.CheckReqVOService;
import com.hnu.legal_cases.service.FavoriteService;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏相关
 *
 * @author cyx
 * @date 2025/9/8
 */
@Slf4j
@RestController
@RequestMapping("/favorite")
public class FavoriteController {
    @Autowired
    FavoriteService favoriteService;
    @Autowired
    CheckReqVOService checkReqVOService;

    /**
     * 添加收藏
     *
     * @param reqVO 添加收藏入参
     * @return 添加收藏结果
     */
    @GetMapping("/add")
    public JSONReturnBean<String> addFavorite(@ModelAttribute FavoriteCaseReqVO reqVO) {
        try {
            checkReqVOService.checkFavoriteCaseVO(reqVO);
            favoriteService.addFavorite(reqVO);
            return JSONReturnBean.success("收藏成功");
        } catch (ServiceException e) {
            log.error("收藏异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("收藏错误，请稍后再试，error=", e);
            return JSONReturnBean.failed("收藏错误，请稍后再试");
        }
    }

    /**
     * 取消收藏
     *
     * @param reqVO 取消收藏入参
     * @return 取消收藏结果
     */
    @DeleteMapping("/delete")
    public JSONReturnBean<String> deleteFavorite(@ModelAttribute FavoriteCaseReqVO reqVO) {
        try {
            checkReqVOService.checkFavoriteCaseVO(reqVO);
            favoriteService.delFavorite(reqVO);
            return JSONReturnBean.success("取消收藏成功");
        } catch (ServiceException e) {
            log.error("取消收藏异常，入参={}，error={}", JSON.toJSONString(reqVO), e.getMessage());
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("取消收藏错误，请稍后再试，error=", e);
            return JSONReturnBean.failed("取消收藏错误，请稍后再试");
        }
    }
}
