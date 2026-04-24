package com.socialmanager.security;

import com.socialmanager.model.User;
import com.socialmanager.repository.UserRepository;
import com.socialmanager.util.JwtUtil;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: google-oauth2-auth
class JwtAuthFilterTest {

    private static JwtAuthFilter jwtAuthFilter;
    private static JwtUtil jwtUtil;
    private static UserRepository userRepository;

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
        userRepository = ctx.getBean(UserRepository.class);
        jwtAuthFilter = ctx.getBean(JwtAuthFilter.class);
    }

    private static String sanitize(String raw, int min, int max) {
        String s = raw.replaceAll("[^a-zA-Z0-9]", "a");
        if (s.isEmpty()) s = "a";
        while (s.length() < min) s += "a";
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }

    // Feature: google-oauth2-auth, Property 1: Valid JWT sets SecurityContext
    @Property(tries = 100)
    void validJwt_setsSecurityContext(
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username) throws Exception {
        userRepository.deleteAllInBatch();
        String safe = sanitize(username, 3, 20);

        User user = User.builder().email(safe + "@test.local").username(safe).password("hashed").name("Test").build();
        userRepository.save(user);

        String token = jwtUtil.generateToken(safe);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        SecurityContextHolder.clearContext();
        jwtAuthFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(safe);

        SecurityContextHolder.clearContext();
        userRepository.deleteAllInBatch();
    }

    // Feature: google-oauth2-auth, Property 2: Invalid or missing JWT leaves SecurityContext empty
    @Property(tries = 100)
    void invalidOrMissingJwt_leavesSecurityContextEmpty(
            @ForAll @NotBlank String randomString) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + randomString.replaceAll("[^a-zA-Z0-9]", "x"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        SecurityContextHolder.clearContext();
        jwtAuthFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        SecurityContextHolder.clearContext();
    }

    // Feature: google-oauth2-auth, Property 19: JwtAuthFilter supports both email and username as JWT subject
    @Property(tries = 100)
    void filter_supportsBothEmailAndUsernameAsSubject(
            @ForAll @NotBlank @StringLength(min = 3, max = 15) String base) throws Exception {
        userRepository.deleteAllInBatch();
        String safe = sanitize(base, 3, 15);

        // Test with username subject (local user)
        User localUser = User.builder().email(safe + "@test.local").username(safe).password("hashed").name("Local").build();
        userRepository.save(localUser);
        String usernameToken = jwtUtil.generateToken(safe);

        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.addHeader("Authorization", "Bearer " + usernameToken);
        SecurityContextHolder.clearContext();
        jwtAuthFilter.doFilter(req1, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        SecurityContextHolder.clearContext();

        // Test with email subject (Google user)
        String email = safe + "@google.com";
        User googleUser = User.builder().email(email).googleId("gid_" + safe).name("Google").build();
        userRepository.save(googleUser);
        String emailToken = jwtUtil.generateToken(email);

        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.addHeader("Authorization", "Bearer " + emailToken);
        SecurityContextHolder.clearContext();
        jwtAuthFilter.doFilter(req2, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        SecurityContextHolder.clearContext();

        userRepository.deleteAllInBatch();
    }
}
