package com.qpoos.erp.auth.dto;

import java.util.UUID;

public record MeResponse(UUID id, String email, String role, boolean emailVerified) {
}
