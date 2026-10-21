package com.qpoos.erp.auth.application;

import com.qpoos.erp.auth.config.AuthProperties;
import com.qpoos.erp.auth.dto.AuthResponse;
import com.qpoos.erp.auth.dto.ForgotPasswordConfirmRequest;
import com.qpoos.erp.auth.dto.ForgotPasswordRequest;
import com.qpoos.erp.auth.dto.LoginRequest;
import com.qpoos.erp.auth.dto.RegisterRequest;
import com.qpoos.erp.auth.dto.VerifyEmailRequest;
import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.common.security.RandomTokenService;
import com.qpoos.erp.common.security.TokenHashService;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import com.qpoos.erp.auth.infrastructure.token.EmailVerificationToken;
import com.qpoos.erp.auth.infrastructure.token.EmailVerificationTokenRepository;
import com.qpoos.erp.auth.infrastructure.token.ForgotPasswordToken;
import com.qpoos.erp.auth.infrastructure.token.ForgotPasswordTokenRepository;
import com.qpoos.erp.auth.infrastructure.token.RefreshToken;
import com.qpoos.erp.auth.infrastructure.token.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE = "If the email exists, a password recovery link has been sent.";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final ForgotPasswordTokenRepository forgotPasswordTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RandomTokenService randomTokenService;
    private final TokenHashService tokenHashService;
    private final DevEmailService emailService;
    private final AuthProperties properties;
    private final Clock clock;

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role("user")
                .isActive(true)
                .emailVerified(false)
                .build();
        UserEntity savedUser = userRepository.save(user);
        createVerificationToken(savedUser);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        OffsetDateTime now = now();
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHash(tokenHashService.hash(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Invalid verification token");
        }

        UserEntity user = token.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);
        token.setUsedAt(now);
        userRepository.save(user);
        emailVerificationTokenRepository.save(token);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Email is not verified");
        }

        user.setLastLoginAt(now());
        userRepository.save(user);
        return issueTokens(user, userAgent, ipAddress);
    }

    @Transactional
    public AuthResponse refresh(
            String rawRefreshToken,
            UUID companyId,
            String userAgent,
            String ipAddress
    ) {
        OffsetDateTime now = now();
        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHash(tokenHashService.hash(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (currentToken.getRevokedAt() != null || currentToken.getExpiresAt().isBefore(now)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UserEntity user = currentToken.getUser();
        if (companyId != null && companyRepository
                .findByIdAndUserIdAndIsActiveTrue(companyId, user.getId())
                .isEmpty()) {
            throw new AccessDeniedException("Company is unavailable or not authorized");
        }

        currentToken.setRevokedAt(now);
        AuthResponse response = issueTokens(user, companyId, userAgent, ipAddress);
        RefreshToken replacement = refreshTokenRepository
                .findByTokenHash(tokenHashService.hash(response.refreshToken()))
                .orElseThrow(() -> new IllegalStateException("Replacement refresh token was not saved"));
        currentToken.setReplacedByTokenId(replacement.getId());
        refreshTokenRepository.save(currentToken);
        return response;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(tokenHashService.hash(rawRefreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request, String ipAddress, String userAgent) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .ifPresent(user -> createForgotPasswordToken(user, ipAddress, userAgent));
        return GENERIC_FORGOT_PASSWORD_MESSAGE;
    }

    @Transactional
    public void confirmForgotPassword(ForgotPasswordConfirmRequest request) {
        OffsetDateTime now = now();
        ForgotPasswordToken token = forgotPasswordTokenRepository
                .findByTokenHash(tokenHashService.hash(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid forgot password token"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Invalid forgot password token");
        }

        UserEntity user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(now);
        revokeAllRefreshTokens(user);
        userRepository.save(user);
        forgotPasswordTokenRepository.save(token);
    }

    private void createVerificationToken(UserEntity user) {
        String rawToken = randomTokenService.generate();
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHashService.hash(rawToken))
                .expiresAt(now().plusMinutes(properties.getEmailVerificationMinutes()))
                .build());
        emailService.sendVerificationLink(user.getEmail(), rawToken);
    }

    private void createForgotPasswordToken(UserEntity user, String ipAddress, String userAgent) {
        String rawToken = randomTokenService.generate();
        forgotPasswordTokenRepository.save(ForgotPasswordToken.builder()
                .user(user)
                .tokenHash(tokenHashService.hash(rawToken))
                .expiresAt(now().plusMinutes(properties.getForgotPasswordMinutes()))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build());
        emailService.sendForgotPasswordLink(user.getEmail(), rawToken);
    }

    private AuthResponse issueTokens(UserEntity user, String userAgent, String ipAddress) {
        return issueTokens(user, null, userAgent, ipAddress);
    }

    private AuthResponse issueTokens(UserEntity user, UUID companyId, String userAgent, String ipAddress) {
        String refreshToken = randomTokenService.generate();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHashService.hash(refreshToken))
                .expiresAt(now().plusDays(properties.getRefreshTokenDays()))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build());
        String accessToken = companyId == null
                ? jwtService.createAccessToken(user)
                : jwtService.createAccessToken(user, companyId);
        return AuthResponse.bearer(accessToken, properties.accessTokenSeconds(), refreshToken);
    }

    private void revokeAllRefreshTokens(UserEntity user) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId());
        OffsetDateTime revokedAt = now();
        activeTokens.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
