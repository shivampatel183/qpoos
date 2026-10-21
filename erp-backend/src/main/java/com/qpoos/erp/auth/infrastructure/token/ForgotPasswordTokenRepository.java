package com.qpoos.erp.auth.infrastructure.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ForgotPasswordTokenRepository extends JpaRepository<ForgotPasswordToken, UUID> {
    Optional<ForgotPasswordToken> findByTokenHash(String tokenHash);
}
