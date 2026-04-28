package com.socialmanager.service;

import com.socialmanager.dto.LoginRequest;
import com.socialmanager.dto.RegisterRequest;
import com.socialmanager.exception.UsernameAlreadyTakenException;
import com.socialmanager.model.User;
import com.socialmanager.repository.UserRepository;
import com.socialmanager.service.auth.AuthService;
import com.socialmanager.util.JwtUtil;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;

// Feature: google-oauth2-auth
class AuthServiceTest {

    private static AuthService authService;
    private static UserRepository userRepository;
    private static JwtUtil jwtUtil;

    // Force the test profile before any Spring context is created.
    // This must happen in a static initializer so it runs before @BeforeContainer.
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

        userRepository = ctx.getBean(UserRepository.class);
        jwtUtil        = ctx.getBean(JwtUtil.class);
        authService    = ctx.getBean(AuthService.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Strip non-alphanumeric chars; pad/trim to [minLen, maxLen]. */
    private static String sanitize(String raw, int minLen, int maxLen) {
        String s = raw.replaceAll("[^a-zA-Z0-9]", "a");
        if (s.isEmpty()) s = "a";
        while (s.length() < minLen) s += "a";
        if (s.length() > maxLen) s = s.substring(0, maxLen);
        return s;
    }

    /** Clear all users using a native delete to bypass JPA first-level cache issues. */
    private static void clearUsers() {
        userRepository.deleteAllInBatch();
    }

    // ── Property 7: processOAuthUser creates a new user for a new email ──────

    // Feature: google-oauth2-auth, Property 7: processOAuthUser creates a new user for a new email
    @Property(tries = 100)
    void processOAuthUser_createsNewUser(@ForAll @NotBlank String localPart,
                                         @ForAll @NotBlank String name,
                                         @ForAll @NotBlank String googleId) {
        clearUsers();
        String email = sanitize(localPart, 1, 30) + "@test.com";

        authService.processOAuthUser(email, name, googleId);

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    // ── Property 8: processOAuthUser is idempotent for existing email ─────────

    // Feature: google-oauth2-auth, Property 8: processOAuthUser is idempotent for existing email
    @Property(tries = 100)
    void processOAuthUser_idempotent(@ForAll @NotBlank String localPart,
                                      @ForAll @NotBlank String name,
                                      @ForAll @NotBlank String googleId) {
        clearUsers();
        String email = sanitize(localPart, 1, 30) + "@idem.com";

        authService.processOAuthUser(email, name, googleId);
        authService.processOAuthUser(email, name, googleId);

        long count = userRepository.findAll().stream()
                .filter(u -> email.equals(u.getEmail())).count();
        assertThat(count).isEqualTo(1);
    }

    // ── Property 13: Duplicate username registration throws ──────────────────

    // Feature: google-oauth2-auth, Property 13: Duplicate username registration returns 409
    @Property(tries = 100)
    void register_duplicateUsername_throws(
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username) {
        clearUsers();
        String safe = sanitize(username, 3, 20);

        RegisterRequest req = new RegisterRequest(safe, safe + "@test.com", "password123", "Test User");
        authService.register(req);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UsernameAlreadyTakenException.class);
    }

    // ── Property 14: Registered user's password is BCrypt hashed ─────────────

    // Feature: google-oauth2-auth, Property 14: Registered user's password is BCrypt hashed
    @Property(tries = 100)
    void register_passwordIsBcryptHashed(
            @ForAll @NotBlank @StringLength(min = 8, max = 20) String password,
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username) {
        clearUsers();
        String safe = sanitize(username, 3, 20);
        // Sanitize password to ASCII-safe chars to stay within BCrypt's 72-byte limit
        String safePass = password.replaceAll("[^\\x20-\\x7E]", "a");
        if (safePass.length() < 8) safePass = safePass + "aaaaaaaa";
        if (safePass.length() > 20) safePass = safePass.substring(0, 20);

        authService.register(new RegisterRequest(safe, safe + "@test.com", safePass, "Test User"));

        User saved = userRepository.findByUsername(safe).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(safePass);
        assertThat(new BCryptPasswordEncoder().matches(safePass, saved.getPassword())).isTrue();
    }

    // ── Property 16: Login with unknown username throws ───────────────────────

    // Feature: google-oauth2-auth, Property 16: Login with unknown username returns 401
    @Property(tries = 100)
    void login_unknownUsername_throws(
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username) {
        clearUsers();
        // Append suffix to guarantee it won't accidentally exist
        String safe = sanitize(username, 3, 20) + "zz";

        assertThatThrownBy(() -> authService.login(new LoginRequest(safe, "somepassword")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── Property 17: Login with wrong password throws ─────────────────────────

    // Feature: google-oauth2-auth, Property 17: Login with wrong password returns 401
    @Property(tries = 100)
    void login_wrongPassword_throws(
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username,
            @ForAll @NotBlank @StringLength(min = 8, max = 20) String correctPassword,
            @ForAll @NotBlank @StringLength(min = 8, max = 20) String wrongPassword) {
        String safeCorrect = correctPassword.replaceAll("[^\\x20-\\x7E]", "a");
        String safeWrong   = wrongPassword.replaceAll("[^\\x20-\\x7E]", "b");
        Assume.that(!safeCorrect.equals(safeWrong));
        clearUsers();
        String safe = sanitize(username, 3, 20);

        authService.register(new RegisterRequest(safe, safeWrong + "@test.com", safeCorrect, "User"));

        assertThatThrownBy(() -> authService.login(new LoginRequest(safe, safeWrong)))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── Property 18: Login round-trip returns usable JWT ─────────────────────

    // Feature: google-oauth2-auth, Property 18: Login round-trip — valid credentials return usable JWT
    @Property(tries = 100)
    void login_roundTrip(
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username,
            @ForAll @NotBlank @StringLength(min = 8, max = 20) String password) {
        clearUsers();
        String safe     = sanitize(username, 3, 20);
        String safePass = password.replaceAll("[^\\x20-\\x7E]", "a");
        if (safePass.length() < 8) safePass = safePass + "aaaaaaaa";
        if (safePass.length() > 20) safePass = safePass.substring(0, 20);

        authService.register(new RegisterRequest(safe, safe + "@test.com", safePass, "User"));
        String token = authService.login(new LoginRequest(safe, safePass));

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(safe);
    }

    // ── Property 20: Local and Google accounts are independent ───────────────

    // Feature: google-oauth2-auth, Property 20: Local and Google accounts are independent
    @Property(tries = 100)
    void localAndGoogleAccounts_areIndependent(
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username,
            @ForAll @NotBlank String googleId) {
        clearUsers();
        String safe = sanitize(username, 3, 20);
        String googleEmail = safe + "@shared.com";

        // Local account — no email
        authService.register(new RegisterRequest(safe, safe + "@local.com", "password123", "Local User"));
        // Google account — separate record with its own email
        authService.processOAuthUser(googleEmail, "Google User", googleId);

        assertThat(userRepository.findByUsername(safe)).isPresent();
        assertThat(userRepository.findByEmail(googleEmail)).isPresent();
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(2);
    }
}
