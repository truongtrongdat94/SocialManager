package com.socialmanager.config;

import com.socialmanager.model.User;
import com.socialmanager.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDemoDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.username:devuser}")
    private String demoUsername;

    @Value("${app.auth.password:devpass123}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User user = userRepository.findByUsername(demoUsername)
                .orElseGet(() -> User.builder()
                        .username(demoUsername)
                        .email(demoUsername + "@local.dev")
                        .name("Dev Demo User")
                        .build());

        if (user.getPassword() == null || user.getPassword().isBlank() || !passwordEncoder.matches(demoPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(demoPassword));
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(demoUsername + "@local.dev");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName("Dev Demo User");
        }

        userRepository.save(user);
    }
}