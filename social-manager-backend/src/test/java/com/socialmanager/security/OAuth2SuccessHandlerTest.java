package com.socialmanager.security;

import com.socialmanager.service.AuthService;
import com.socialmanager.util.JwtUtil;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Feature: google-oauth2-auth
class OAuth2SuccessHandlerTest {

    private static JwtUtil jwtUtil;
    private static AuthService authService;

    static {
        System.setProperty("spring.profiles.active", "test");
    }

    @BeforeContainer
    static void startSpring() {
        SpringApplication app = new SpringApplication(
                com.socialmanager.SocialManagerBackendApplication.class,
                com.socialmanager.config.TestSecurityConfig.class
        );
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run();
        jwtUtil = ctx.getBean(JwtUtil.class);
        authService = ctx.getBean(AuthService.class);
    }

    // Feature: google-oauth2-auth, Property 6: OAuth2 success handler redirect contains JWT
    @Property(tries = 100)
    void successHandler_redirectContainsValidJwt(
            @ForAll @NotBlank @StringLength(min = 1, max = 30) String emailLocal,
            @ForAll @NotBlank @StringLength(min = 1, max = 50) String name,
            @ForAll @NotBlank @StringLength(min = 1, max = 50) String sub) throws Exception {
        String email = emailLocal.replaceAll("[^a-zA-Z0-9]", "a") + "@test.com";

        // Build OidcUser with the given claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("email", email);
        claims.put("name", name);
        claims.put("iat", Instant.now());
        claims.put("exp", Instant.now().plusSeconds(3600));
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        OidcUser oidcUser = new DefaultOidcUser(Collections.emptyList(), idToken);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oidcUser);

        OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(authService, jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, auth);

        String redirectUrl = response.getRedirectedUrl();
        assertThat(redirectUrl).startsWith("http://localhost:3000/auth/callback?token=");

        String token = redirectUrl.substring("http://localhost:3000/auth/callback?token=".length());
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(email);
    }
}
