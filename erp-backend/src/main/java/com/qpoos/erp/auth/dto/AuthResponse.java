package com.qpoos.erp.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        @JsonIgnore
        String refreshToken
) {
    public static AuthResponse bearer(String accessToken, long expiresInSeconds, String refreshToken) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, refreshToken);
    }
}
