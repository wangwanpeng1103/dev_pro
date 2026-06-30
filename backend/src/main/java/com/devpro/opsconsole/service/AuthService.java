package com.devpro.opsconsole.service;

import com.devpro.opsconsole.model.UserAccount;
import com.devpro.opsconsole.repository.UserAdminRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * 认证业务服务，负责登录校验和密码匹配边界，后续可独立替换为 Spring Security。
 */
@Service
public class AuthService {

    private final UserAdminRepository userAdminRepository;

    public AuthService(UserAdminRepository userAdminRepository) {
        this.userAdminRepository = userAdminRepository;
    }

    /**
     * 登录并返回当前用户。第一期使用简单密码校验，后续可替换为正式密码策略。
     *
     * @param username 用户名
     * @param password 密码
     * @return 当前用户
     */
    public UserAccount login(String username, String password) {
        UserAccount userAccount = userAdminRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在、已禁用或已过期"));
        if (!userAccount.canLogin(LocalDateTime.now())) {
            throw new IllegalArgumentException("用户不存在、已禁用或已过期");
        }
        String passwordHash = userAdminRepository.findPasswordHashByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在、已禁用或已过期"));
        if (!matchesPassword(password, passwordHash)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return userAccount;
    }

    private boolean matchesPassword(String rawPassword, String passwordHash) {
        if (passwordHash != null && passwordHash.startsWith("{noop}")) {
            return passwordHash.substring("{noop}".length()).equals(rawPassword);
        }
        return false;
    }
}
