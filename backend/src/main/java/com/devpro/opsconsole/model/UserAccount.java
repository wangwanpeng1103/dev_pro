package com.devpro.opsconsole.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 运维控制台用户账号模型，当前阶段由内存数据承载，后续可映射到 sys_user 表。
 */
public class UserAccount {

    private final Long id;
    private final String username;
    private final String displayName;
    private final UserType userType;
    private final LocalDateTime expiresAt;
    private boolean enabled;
    private final Set<String> projectCodes = new LinkedHashSet<>();

    public UserAccount(Long id, String username, String displayName, UserType userType, LocalDateTime expiresAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.userType = userType;
        this.expiresAt = expiresAt;
        this.enabled = true;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserType getUserType() {
        return userType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getProjectCodes() {
        return projectCodes;
    }

    /**
     * 判断用户当前是否允许登录，临时用户需要额外校验有效期。
     *
     * @param now 当前时间
     * @return 是否可登录
     */
    public boolean canLogin(LocalDateTime now) {
        return enabled && (expiresAt == null || expiresAt.isAfter(now));
    }
}

