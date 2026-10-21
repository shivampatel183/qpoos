package com.qpoos.erp.auth.api;

import com.qpoos.erp.auth.config.AuthProperties;
import com.qpoos.erp.auth.application.AuthService;
import com.qpoos.erp.auth.dto.AuthResponse;
import com.qpoos.erp.auth.dto.ForgotPasswordConfirmRequest;
import com.qpoos.erp.auth.dto.ForgotPasswordRequest;
import com.qpoos.erp.auth.dto.LoginRequest;
import com.qpoos.erp.auth.dto.MeResponse;
import com.qpoos.erp.auth.dto.MessageResponse;
import com.qpoos.erp.auth.dto.RegisterRequest;
import com.qpoos.erp.auth.dto.RefreshAccessTokenRequest;
import com.qpoos.erp.auth.dto.VerifyEmailRequest;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthProperties properties;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new MessageResponse("Registration successful. Please verify your email."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(new MessageResponse("Email verified successfully."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthResponse response = authService.login(request, userAgent(servletRequest), ipAddress(servletRequest));
        return withRefreshCookie(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshAccessTokenRequest refreshRequest,
            HttpServletRequest request
    ) {
        UUID companyId = refreshRequest == null ? null : refreshRequest.companyId();
        AuthResponse response = authService.refresh(
                readRefreshCookie(request),
                companyId,
                userAgent(request),
                ipAddress(request)
        );
        return withRefreshCookie(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(readRefreshCookieOrNull(request));
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        UUID userId = SecurityUtils.getUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                Boolean.TRUE.equals(user.getEmailVerified())
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        String message = authService.forgotPassword(request, ipAddress(servletRequest), userAgent(servletRequest));
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @PostMapping("/forgot-password/confirm")
    public ResponseEntity<MessageResponse> confirmForgotPassword(@Valid @RequestBody ForgotPasswordConfirmRequest request) {
        authService.confirmForgotPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password updated successfully."));
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(response.refreshToken()).toString())
                .body(response);
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from(properties.getRefreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(properties.getRefreshTokenDays()))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(properties.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String readRefreshCookie(HttpServletRequest request) {
        String token = readRefreshCookieOrNull(request);
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Refresh token cookie is missing");
        }
        return token;
    }

    private String readRefreshCookieOrNull(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> properties.getRefreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String ipAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
