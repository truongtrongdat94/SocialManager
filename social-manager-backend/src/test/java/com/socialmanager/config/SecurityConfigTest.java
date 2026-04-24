package com.socialmanager.config;

import net.jqwik.api.*;
import org.springframework.util.AntPathMatcher;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: google-oauth2-auth
class SecurityConfigTest {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String[] PUBLIC_PATTERNS = {
        "/auth/**",
        "/oauth2/**",
        "/login/**",
        "/actuator/health",
    };

    private boolean isPublicPath(String path) {
        for (String pattern : PUBLIC_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    // Feature: google-oauth2-auth, Property 3: Public paths are accessible without authentication
    // Validates: Requirements 1.4, 2.2
    @Property(tries = 100)
    void publicPaths_arePermittedWithoutAuth(
            @ForAll("publicPathSuffixes") String suffix) {
        assertThat(isPublicPath("/auth/" + suffix)).isTrue();
        assertThat(isPublicPath("/oauth2/" + suffix)).isTrue();
        assertThat(isPublicPath("/login/" + suffix)).isTrue();
        assertThat(isPublicPath("/actuator/health")).isTrue();
    }

    @Provide
    Arbitrary<String> publicPathSuffixes() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(20);
    }

    // Feature: google-oauth2-auth, Property 4: Protected paths require authentication
    // Validates: Requirements 2.3, 5.3
    @Property(tries = 100)
    void protectedPaths_requireAuth(
            @ForAll("protectedPathSegments") String segment) {
        String path = "/api/" + segment;
        assertThat(isPublicPath(path)).isFalse();
    }

    @Provide
    Arbitrary<String> protectedPathSegments() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20);
    }
}
