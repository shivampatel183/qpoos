package com.qpoos.erp.auth.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Dev-only email sender — logs links to the console instead of sending real emails. */
@Service
public class DevEmailService {

    private static final Logger log = LoggerFactory.getLogger(DevEmailService.class);
    private final String frontendUrl;

    public DevEmailService(@Value("${app.auth.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public void sendVerificationLink(String email, String token) {
        log.info("Email verification link for {}: {}/verify-email?token={}", email, frontendUrl, token);
    }

    public void sendForgotPasswordLink(String email, String token) {
        log.info("Forgot password link for {}: {}/forgot-password?token={}", email, frontendUrl, token);
    }
}
