package com.insightflow.model;

/**
 * Lightweight security principal built from JWT claims — no DB hit required.
 * Set as the principal in SecurityContextHolder on every authenticated request.
 */
public record UserPrincipal(Long id, String email, String role) {}
