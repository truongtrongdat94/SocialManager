package com.socialmanager.service;

import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.User;
import com.socialmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("Unable to resolve current user");
        }

        String principal = authentication.getName().trim();
        return userRepository.findByEmailIgnoreCaseOrNameIgnoreCase(principal, principal)
                .orElseThrow(() -> new BusinessException("Current user not found"));
    }
}