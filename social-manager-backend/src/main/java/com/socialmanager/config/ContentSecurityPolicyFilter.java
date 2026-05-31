package com.socialmanager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter CSP chuẩn cho SocialManager.
 * Cho phép TikTok login, captcha, SDK chạy ổn định.
 */
@Component
public class ContentSecurityPolicyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // CSP: chỉ whitelist TikTok/Bytedance + các domain cần thiết
        String csp = "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                "*.tiktokcdn.com *.ibytedtos.com " +
                "s20.tiktokcdn.com lf16-cdn-tos.tiktokcdn-us.com " +
                "sf16-website.neutral.ttwstatic.com; " +
                "connect-src 'self' *.tiktokcdn.com *.ibytedtos.com; " +
                "img-src 'self' data: *.tiktokcdn.com *.ibytedtos.com; " +
                "style-src 'self' 'unsafe-inline'; " +
                "font-src 'self'; " +
                "frame-src 'self' *.tiktokcdn.com;";

        response.setHeader("Content-Security-Policy", csp);

        filterChain.doFilter(request, response);
    }
}