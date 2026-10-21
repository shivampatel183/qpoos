package com.qpoos.erp.common.security;

import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.UUID;

/**
 * Immutable value object representing the currently authenticated user.
 * Populated from JWT claims only — no DB round-trip per request.
 */
public record UserPrincipal(
        UUID id,
        String email,
        String role,
        UUID companyId,
        Collection<? extends GrantedAuthority> authorities
) implements Principal {

    @Override
    public String getName() {
        return id.toString();
    }
}
