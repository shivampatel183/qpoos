package com.qpoos.erp.company.dto;

import com.qpoos.erp.auth.dto.AuthResponse;

public record CompanyAuthResponse (
    String accessToken,
    String tokenType,
    long expiresInSeconds
){
    public static CompanyAuthResponse bearer(String accessToken, long expiresInSeconds) {
        return new CompanyAuthResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
