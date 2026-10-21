package com.qpoos.erp.common.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/** Generates cryptographically secure random URL-safe tokens for refresh/verify/reset flows. */
@Service
public class RandomTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
