package com.hnu.legal_cases.dao;

import com.hnu.legal_cases.pojo.Users;
import org.apache.ibatis.annotations.Param;

/**
 * users 表对应 Mapper
 *
 * @author baixu
 * @date 2025/9/6
 */
public interface UserMapper {

    /**
     * 注册新用户
     *
     * @param username 用户名
     * @param email    用户邮箱
     * @param password 用户密码
     */
    void insert(@Param("username") String username, @Param("email") String email, @Param("password") String password);

    /**
     * 检查用户名是否已存在
     *
     * @param username 待检查的用户名
     * @return true-用户名已存在，false-用户名不存在
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 待检查的邮箱地址
     * @return true-邮箱已存在，false-邮箱不存在
     */
    boolean existsByEmail(@Param("email") String email);

    /**
     * 更新用户最后登录时间
     *
     * @param userId 用户ID
     */
    void updateLastLoginDate(@Param("userId") Long userId);

    /**
     * 通过邮箱和密码进行用户登录验证
     *
     * @param email    用户邮箱
     * @param password 用户密码
     * @return 匹配的用户信息，如果验证失败则返回null
     */
    Users loginByEmail(@Param("email") String email, @Param("password") String password);

    /**
     * 扣减 1 次摘要额度（仅当 summary_credits &gt; 0 时生效）
     *
     * @return 受影响行数，0 表示额度不足
     */
    int decrementSummaryCredits(@Param("userId") Long userId);

    /**
     * 退回 1 次摘要额度（生成失败时）
     */
    void incrementSummaryCredits(@Param("userId") Long userId);

    /**
     * 查询当前剩余摘要次数
     */
    Integer selectSummaryCreditsByUserId(@Param("userId") Long userId);

    /**
     * 增加摘要次数（充值、管理员发放）
     */
    int addSummaryCredits(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 按邮箱查询已激活用户（不校验密码）
     */
    Users findActiveByEmail(@Param("email") String email);

    /**
     * 更新登录密码（明文，与现网一致）
     */
    int updatePasswordByEmail(@Param("email") String email, @Param("password") String password);

}
