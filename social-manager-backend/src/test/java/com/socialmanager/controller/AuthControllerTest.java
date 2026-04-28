package com.socialmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmanager.dto.LoginRequest;
import com.socialmanager.dto.RegisterRequest;
import com.socialmanager.dto.UserDto;
import com.socialmanager.exception.GlobalExceptionHandler;
import com.socialmanager.repository.UserRepository;
import com.socialmanager.service.auth.AuthService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Feature: google-oauth2-auth
class AuthControllerTest {

    private static MockMvc mockMvc;
    private static AuthService authService;
    private static UserRepository userRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

        authService = ctx.getBean(AuthService.class);
        userRepository = ctx.getBean(UserRepository.class);
        AuthController controller = ctx.getBean(AuthController.class);
        GlobalExceptionHandler exceptionHandler = ctx.getBean(GlobalExceptionHandler.class);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    private static String sanitize(String raw, int min, int max) {
        String s = raw.replaceAll("[^a-zA-Z0-9]", "a");
        if (s.isEmpty()) s = "a";
        while (s.length() < min) s += "a";
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }

    // Feature: google-oauth2-auth, Property 15: Password shorter than 8 characters returns 400
    @Property(tries = 100)
    void register_shortPassword_returns400(
            @ForAll @NotBlank @StringLength(min = 1, max = 7) String shortPassword,
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username) throws Exception {
        userRepository.deleteAllInBatch();
        String safe = sanitize(username, 3, 20);
        String safePass = sanitize(shortPassword, 1, 7);
        if (safePass.length() >= 8) safePass = safePass.substring(0, 7);

        RegisterRequest req = new RegisterRequest(safe, safe + "@test.com", safePass, "Test User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // Feature: google-oauth2-auth, Property 9: GET /me returns correct UserDto for authenticated user
    // Tested via service layer (controller /me requires Authentication principal — tested via integration)
    @Property(tries = 100)
    void getCurrentUser_returnsCorrectUserDto(
            @ForAll @NotBlank @StringLength(min = 3, max = 20) String username) {
        userRepository.deleteAllInBatch();
        String safe = sanitize(username, 3, 20);
        authService.register(new RegisterRequest(safe, safe + "@test.com", "password123", "Test User"));

        UserDto dto = authService.getCurrentUser(safe);
        assertThat(dto.username()).isEqualTo(safe);
        assertThat(dto.name()).isEqualTo("Test User");
    }
}
