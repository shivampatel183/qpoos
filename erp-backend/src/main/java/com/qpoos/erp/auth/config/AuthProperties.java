package com.qpoos.erp.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String jwtSecret;
    private long accessTokenMinutes;
    private long refreshTokenDays;
    private long emailVerificationMinutes;
    private long forgotPasswordMinutes;
    private String refreshCookieName;
    private boolean refreshCookieSecure;
    private String frontendUrl;

    public long accessTokenSeconds() {
        return accessTokenMinutes * 60;
    }
}
