package com.devpro.opsconsole.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * 运维控制台用户账号模型，用于承载登录状态、用户类型和项目授权。
 */
@Getter
public class UserAccount {

    private final Long id;
    private final String username;
    private final String displayName;
    private final UserType userType;
    private final LocalDateTime expiresAt;
    @Setter
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

    /**
     * 判断用户当前是否允许登录：临时用户只校验有效期，永久用户和管理员校验启用状态。
     *
     * @param now 当前时间
     * @return 是否可登录
     */
    public boolean canLogin(LocalDateTime now) {
        if (userType == UserType.TEMPORARY) {
            return expiresAt != null && expiresAt.isAfter(now);
        }
        return enabled;
    }
}
