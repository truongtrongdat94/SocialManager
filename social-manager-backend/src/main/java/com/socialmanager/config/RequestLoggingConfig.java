package com.socialmanager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Configuration
public class RequestLoggingConfig {

    @Bean
    public OncePerRequestFilter requestLoggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                long start = System.currentTimeMillis();
                chain.doFilter(request, response);
                long duration = System.currentTimeMillis() - start;
                log.info("{} {} -> {} ({}ms)",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        duration);
            }
        };
    }
}
