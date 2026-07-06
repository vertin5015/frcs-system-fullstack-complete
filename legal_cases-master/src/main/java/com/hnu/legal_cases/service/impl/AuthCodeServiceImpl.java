package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.UserMapper;
import com.hnu.legal_cases.dto.auth.SendCodeResVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.Users;
import com.hnu.legal_cases.service.AuthCodeService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCodeServiceImpl implements AuthCodeService {

    private static final String PURPOSE_LOGIN = "LOGIN";
    private static final String PURPOSE_RESET = "RESET";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;

    @Value("${app.auth.code-ttl-seconds:600}")
    private int codeTtlSeconds;
    @Value("${app.auth.send-cooldown-seconds:60}")
    private int cooldownSeconds;
    @Value("${app.auth.expose-code-in-response:false}")
    private boolean exposeCodeInResponse;

    private final SecureRandom random = new SecureRandom();

    private static String codeKey(String purpose, String email) {
        return "auth:code:" + purpose + ":" + email.trim().toLowerCase();
    }

    private static String cooldownKey(String purpose, String email) {
        return "auth:cd:" + purpose + ":" + email.trim().toLowerCase();
    }

    @Override
    public SendCodeResVO sendCode(String email, String purpose) {
        if (StringUtils.isBlank(email)) {
            throw new ServiceException("邮箱为空");
        }
        if (!PURPOSE_LOGIN.equals(purpose) && !PURPOSE_RESET.equals(purpose)) {
            throw new ServiceException("无效的用途，仅支持 LOGIN 或 RESET");
        }
        Users u = userMapper.findActiveByEmail(email.trim());
        if (u == null) {
            throw new ServiceException("该邮箱未注册或未激活");
        }

        String cd = cooldownKey(purpose, email);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cd))) {
            throw new ServiceException("发送过于频繁，请 " + cooldownSeconds + " 秒后再试");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        stringRedisTemplate.opsForValue().set(codeKey(purpose, email), code, Duration.ofSeconds(codeTtlSeconds));
        stringRedisTemplate.opsForValue().set(cd, "1", Duration.ofSeconds(cooldownSeconds));

        log.warn("[验证码] email={} purpose={} code={} （生产环境请配置邮件或关闭日志级别）", email.trim(), purpose, code);

        SendCodeResVO vo = new SendCodeResVO();
        vo.setCooldownSeconds(cooldownSeconds);
        if (exposeCodeInResponse) {
            vo.setDevCode(code);
        }
        return vo;
    }

    @Override
    public void verifyAndDelete(String email, String purpose, String code) {
        if (StringUtils.isBlank(email) || StringUtils.isBlank(code)) {
            throw new ServiceException("邮箱或验证码为空");
        }
        String key = codeKey(purpose, email);
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new ServiceException("验证码已过期或未发送");
        }
        if (!stored.equals(code.trim())) {
            throw new ServiceException("验证码错误");
        }
        stringRedisTemplate.delete(key);
    }
}
