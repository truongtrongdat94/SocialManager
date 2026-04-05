package com.socialmanager.security;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: google-oauth2-auth
class CustomOAuth2UserServiceTest {

    // Feature: google-oauth2-auth, Property 5: OidcUser attribute extraction is complete
    @Property(tries = 100)
    void oidcUser_attributeExtractionIsComplete(
            @ForAll @NotBlank String email,
            @ForAll @NotBlank String name,
            @ForAll @NotBlank String sub) {
        // Build a mock OidcIdToken with the required claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("email", email);
        claims.put("name", name);
        claims.put("iat", Instant.now());
        claims.put("exp", Instant.now().plusSeconds(3600));

        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );

        OidcUser oidcUser = new DefaultOidcUser(Collections.emptyList(), idToken);

        // Verify all three attributes are extractable without loss
        assertThat(oidcUser.getEmail()).isEqualTo(email);
        assertThat(oidcUser.getFullName()).isEqualTo(name);
        assertThat(oidcUser.getSubject()).isEqualTo(sub);
    }
}
