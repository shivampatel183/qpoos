package com.qpoos.erp.common.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes raw opaque tokens (refresh tokens, verify tokens, etc.) using SHA-256
 * before persisting them to the database.
 *
 * <p>Only the hash is ever stored — the plaintext token is sent to the user once
 * and never retained server-side.</p>
 */
@Service
public class TokenHashService {

    private static final String ALGORITHM = "SHA-256";

    /** Returns the lower-case hex SHA-256 digest of {@code rawToken}. */
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the Java SE spec — should never happen
            throw new IllegalStateException(ALGORITHM + " algorithm is unavailable on this JVM", ex);
        }
    }
}
