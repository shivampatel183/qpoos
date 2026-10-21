package com.qpoos.erp.auth;

import com.qpoos.erp.auth.application.AuthService;
import com.qpoos.erp.auth.application.DevEmailService;
import com.qpoos.erp.auth.config.AuthProperties;
import com.qpoos.erp.auth.dto.AuthResponse;
import com.qpoos.erp.auth.dto.ForgotPasswordConfirmRequest;
import com.qpoos.erp.auth.dto.ForgotPasswordRequest;
import com.qpoos.erp.auth.dto.LoginRequest;
import com.qpoos.erp.auth.dto.RegisterRequest;
import com.qpoos.erp.auth.dto.VerifyEmailRequest;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import com.qpoos.erp.auth.infrastructure.token.EmailVerificationToken;
import com.qpoos.erp.auth.infrastructure.token.EmailVerificationTokenRepository;
import com.qpoos.erp.auth.infrastructure.token.ForgotPasswordToken;
import com.qpoos.erp.auth.infrastructure.token.ForgotPasswordTokenRepository;
import com.qpoos.erp.auth.infrastructure.token.RefreshToken;
import com.qpoos.erp.auth.infrastructure.token.RefreshTokenRepository;
import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.common.security.RandomTokenService;
import com.qpoos.erp.common.security.TokenHashService;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-15T18:30:00Z"), ZoneOffset.UTC);
    private final AuthProperties properties = new AuthProperties();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final TokenHashService tokenHashService = new TokenHashService();
    private final List<UserEntity> users = new ArrayList<>();
    private final List<CompanyEntity> companies = new ArrayList<>();
    private final List<RefreshToken> refreshTokens = new ArrayList<>();
    private final List<EmailVerificationToken> emailTokens = new ArrayList<>();
    private final List<ForgotPasswordToken> forgotTokens = new ArrayList<>();
    private AuthService authService;
    private JwtService jwtService;
    private RecordingEmailService emailService;

    @BeforeEach
    void setUp() {
        properties.setJwtSecret("dev-only-change-this-secret-dev-only-change-this-secret");
        properties.setAccessTokenMinutes(15);
        properties.setRefreshTokenDays(30);
        properties.setEmailVerificationMinutes(1440);
        properties.setForgotPasswordMinutes(15);
        properties.setRefreshCookieName("refresh_token");
        properties.setRefreshCookieSecure(false);
        properties.setFrontendUrl("http://localhost:8080");

        UserRepository userRepository = mockUserRepository();
        RefreshTokenRepository refreshTokenRepository = mockRefreshTokenRepository();
        EmailVerificationTokenRepository emailVerificationTokenRepository = mockEmailVerificationTokenRepository();
        ForgotPasswordTokenRepository forgotPasswordTokenRepository = mockForgotPasswordTokenRepository();

        emailService = new RecordingEmailService();
        jwtService = new JwtService(properties, clock);
        jwtService.init();

        authService = new AuthService(
                userRepository,
                mockCompanyRepository(),
                refreshTokenRepository,
                emailVerificationTokenRepository,
                forgotPasswordTokenRepository,
                passwordEncoder,
                jwtService,
                new RandomTokenService(),
                tokenHashService,
                emailService,
                properties,
                clock
        );
    }

    @Test
    void registerCreatesUnverifiedUserAndVerificationToken() {
        authService.register(new RegisterRequest("User@Example.com", "StrongPass123!"));

        UserEntity user = findUser("user@example.com").orElseThrow();
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getEmailVerified()).isFalse();
        assertThat(passwordEncoder.matches("StrongPass123!", user.getPasswordHash())).isTrue();
        assertThat(emailTokens).hasSize(1);
        assertThat(emailService.verificationLinks).hasSize(1);
    }

    @Test
    void loginBeforeEmailVerificationIsRejected() {
        authService.register(new RegisterRequest("user@example.com", "StrongPass123!"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@example.com", "StrongPass123!"),
                "JUnit",
                "127.0.0.1"
        )).hasMessageContaining("Email is not verified");
    }

    @Test
    void verifyEmailAllowsLoginAndCreatesRefreshToken() {
        authService.register(new RegisterRequest("user@example.com", "StrongPass123!"));
        authService.verifyEmail(new VerifyEmailRequest(emailService.verificationTokens.get(0)));

        AuthResponse response = authService.login(
                new LoginRequest("user@example.com", "StrongPass123!"),
                "JUnit",
                "127.0.0.1"
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.get(0).getRevokedAt()).isNull();
    }

    @Test
    void refreshRotatesRefreshTokenAndRevokesPreviousToken() {
        authService.register(new RegisterRequest("user@example.com", "StrongPass123!"));
        authService.verifyEmail(new VerifyEmailRequest(emailService.verificationTokens.get(0)));
        AuthResponse first = authService.login(new LoginRequest("user@example.com", "StrongPass123!"), "JUnit", "127.0.0.1");

        AuthResponse second = authService.refresh(first.refreshToken(), null, "JUnit2", "127.0.0.2");

        assertThat(second.accessToken()).isNotBlank();
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(refreshTokens).hasSize(2);
        assertThat(findRefreshToken(tokenHashService.hash(first.refreshToken())).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(findRefreshToken(tokenHashService.hash(second.refreshToken())).orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void refreshWithoutCompanyCreatesCompanyLessAccessToken() {
        UserEntity user = registeredVerifiedUser();
        AuthResponse first = authService.login(
                new LoginRequest(user.getEmail(), "StrongPass123!"),
                "JUnit",
                "127.0.0.1"
        );

        AuthResponse refreshed = authService.refresh(
                first.refreshToken(),
                null,
                "JUnit2",
                "127.0.0.2"
        );

        Claims claims = jwtService.validateAndGetClaims(refreshed.accessToken());
        assertThat(claims.get("companyId")).isNull();
    }

    @Test
    void refreshWithOwnedActiveCompanyCreatesCompanyScopedAccessToken() {
        UserEntity user = registeredVerifiedUser();
        CompanyEntity company = saveCompany(user.getId(), true);
        AuthResponse first = authService.login(
                new LoginRequest(user.getEmail(), "StrongPass123!"),
                "JUnit",
                "127.0.0.1"
        );

        AuthResponse refreshed = authService.refresh(
                first.refreshToken(),
                company.getId(),
                "JUnit2",
                "127.0.0.2"
        );

        Claims claims = jwtService.validateAndGetClaims(refreshed.accessToken());
        assertThat(claims.get("companyId", String.class)).isEqualTo(company.getId().toString());
    }

    @Test
    void invalidCompanyDoesNotRevokeCurrentRefreshToken() {
        UserEntity user = registeredVerifiedUser();
        CompanyEntity otherUsersCompany = saveCompany(UUID.randomUUID(), true);
        AuthResponse first = authService.login(
                new LoginRequest(user.getEmail(), "StrongPass123!"),
                "JUnit",
                "127.0.0.1"
        );

        assertThatThrownBy(() -> authService.refresh(
                first.refreshToken(),
                otherUsersCompany.getId(),
                "JUnit2",
                "127.0.0.2"
        )).isInstanceOf(AccessDeniedException.class);

        assertThat(findRefreshToken(tokenHashService.hash(first.refreshToken())).orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void inactiveCompanyIsRejectedWithoutRevokingCurrentRefreshToken() {
        UserEntity user = registeredVerifiedUser();
        CompanyEntity inactiveCompany = saveCompany(user.getId(), false);
        AuthResponse first = authService.login(
                new LoginRequest(user.getEmail(), "StrongPass123!"),
                "JUnit",
                "127.0.0.1"
        );

        assertThatThrownBy(() -> authService.refresh(
                first.refreshToken(),
                inactiveCompany.getId(),
                "JUnit2",
                "127.0.0.2"
        )).isInstanceOf(AccessDeniedException.class);

        assertThat(findRefreshToken(tokenHashService.hash(first.refreshToken())).orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void forgotPasswordUsesGenericResponseAndConfirmationRevokesSessions() {
        authService.register(new RegisterRequest("user@example.com", "StrongPass123!"));
        authService.verifyEmail(new VerifyEmailRequest(emailService.verificationTokens.get(0)));
        AuthResponse login = authService.login(new LoginRequest("user@example.com", "StrongPass123!"), "JUnit", "127.0.0.1");

        String missingMessage = authService.forgotPassword(new ForgotPasswordRequest("missing@example.com"), "127.0.0.1", "JUnit");
        String existingMessage = authService.forgotPassword(new ForgotPasswordRequest("user@example.com"), "127.0.0.1", "JUnit");
        authService.confirmForgotPassword(new ForgotPasswordConfirmRequest(emailService.forgotPasswordTokens.get(0), "NewStrongPass123!"));

        UserEntity user = findUser("user@example.com").orElseThrow();
        assertThat(existingMessage).isEqualTo(missingMessage);
        assertThat(passwordEncoder.matches("NewStrongPass123!", user.getPasswordHash())).isTrue();
        assertThat(refreshTokens).allMatch(token -> token.getRevokedAt() != null);
        assertThatThrownBy(() -> authService.refresh(login.refreshToken(), null, "JUnit", "127.0.0.1"))
                .hasMessageContaining("Invalid refresh token");
    }

    private UserEntity registeredVerifiedUser() {
        authService.register(new RegisterRequest("user@example.com", "StrongPass123!"));
        authService.verifyEmail(new VerifyEmailRequest(emailService.verificationTokens.get(0)));
        return findUser("user@example.com").orElseThrow();
    }

    private CompanyEntity saveCompany(UUID userId, boolean active) {
        CompanyEntity company = CompanyEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Test Company")
                .isActive(active)
                .build();
        companies.add(company);
        return company;
    }

    private UserRepository mockUserRepository() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase(any())).thenAnswer(invocation -> findUser(invocation.getArgument(0)));
        when(repository.existsByEmailIgnoreCase(any())).thenAnswer(invocation -> findUser(invocation.getArgument(0)).isPresent());
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
                user.setCreatedAt(OffsetDateTime.now(clock));
            }
            user.setUpdatedAt(OffsetDateTime.now(clock));
            users.removeIf(existing -> existing.getId().equals(user.getId()));
            users.add(user);
            return user;
        });
        return repository;
    }

    private RefreshTokenRepository mockRefreshTokenRepository() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        when(repository.findByTokenHash(any())).thenAnswer(invocation -> findRefreshToken(invocation.getArgument(0)));
        when(repository.findAllByUserIdAndRevokedAtIsNull(any())).thenAnswer(invocation -> {
            UUID userId = invocation.getArgument(0);
            return refreshTokens.stream()
                    .filter(token -> token.getUser().getId().equals(userId))
                    .filter(token -> token.getRevokedAt() == null)
                    .toList();
        });
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
                token.setCreatedAt(OffsetDateTime.now(clock));
            }
            refreshTokens.removeIf(existing -> existing.getId().equals(token.getId()));
            refreshTokens.add(token);
            return token;
        });
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<RefreshToken> tokens = invocation.getArgument(0);
            List<RefreshToken> saved = new ArrayList<>();
            tokens.forEach(token -> {
                refreshTokens.removeIf(existing -> existing.getId().equals(token.getId()));
                refreshTokens.add(token);
                saved.add(token);
            });
            return saved;
        });
        return repository;
    }

    private CompanyRepository mockCompanyRepository() {
        CompanyRepository repository = mock(CompanyRepository.class);
        when(repository.findByIdAndUserIdAndIsActiveTrue(any(), any())).thenAnswer(invocation -> {
            UUID companyId = invocation.getArgument(0);
            UUID userId = invocation.getArgument(1);
            return companies.stream()
                    .filter(company -> company.getId().equals(companyId))
                    .filter(company -> company.getUserId().equals(userId))
                    .filter(company -> Boolean.TRUE.equals(company.getIsActive()))
                    .findFirst();
        });
        return repository;
    }

    private EmailVerificationTokenRepository mockEmailVerificationTokenRepository() {
        EmailVerificationTokenRepository repository = mock(EmailVerificationTokenRepository.class);
        when(repository.findByTokenHash(any())).thenAnswer(invocation -> findEmailToken(invocation.getArgument(0)));
        when(repository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> {
            EmailVerificationToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
                token.setCreatedAt(OffsetDateTime.now(clock));
            }
            emailTokens.removeIf(existing -> existing.getId().equals(token.getId()));
            emailTokens.add(token);
            return token;
        });
        return repository;
    }

    private ForgotPasswordTokenRepository mockForgotPasswordTokenRepository() {
        ForgotPasswordTokenRepository repository = mock(ForgotPasswordTokenRepository.class);
        when(repository.findByTokenHash(any())).thenAnswer(invocation -> findForgotToken(invocation.getArgument(0)));
        when(repository.save(any(ForgotPasswordToken.class))).thenAnswer(invocation -> {
            ForgotPasswordToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
                token.setCreatedAt(OffsetDateTime.now(clock));
            }
            forgotTokens.removeIf(existing -> existing.getId().equals(token.getId()));
            forgotTokens.add(token);
            return token;
        });
        return repository;
    }

    private Optional<UserEntity> findUser(String email) {
        return users.stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    private Optional<RefreshToken> findRefreshToken(String tokenHash) {
        return refreshTokens.stream().filter(token -> token.getTokenHash().equals(tokenHash)).findFirst();
    }

    private Optional<EmailVerificationToken> findEmailToken(String tokenHash) {
        return emailTokens.stream().filter(token -> token.getTokenHash().equals(tokenHash)).findFirst();
    }

    private Optional<ForgotPasswordToken> findForgotToken(String tokenHash) {
        return forgotTokens.stream().filter(token -> token.getTokenHash().equals(tokenHash)).findFirst();
    }

    private static final class RecordingEmailService extends DevEmailService {
        private final List<String> verificationLinks = new ArrayList<>();
        private final List<String> verificationTokens = new ArrayList<>();
        private final List<String> forgotPasswordTokens = new ArrayList<>();

        RecordingEmailService() {
            super("http://localhost:8080");
        }

        @Override
        public void sendVerificationLink(String email, String token) {
            verificationTokens.add(token);
            verificationLinks.add("verify:" + email + ":" + token);
        }

        @Override
        public void sendForgotPasswordLink(String email, String token) {
            forgotPasswordTokens.add(token);
        }
    }
}
