package com.socialmanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components()
                .addSecuritySchemes("Bearer", new SecurityScheme()
                    .name("Bearer")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }

    /**
     * Serve frontend static files from Spring Boot.
     * Frontend build output should be at classpath:/static/
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static assets (JS, CSS, images)
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(0);

        registry.addResourceHandler("/*.js", "/*.css")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);

        // Serve index.html at root
        registry.addResourceHandler("/")
                .addResourceLocations("classpath:/static/index.html");
    }

    /**
     * Redirect root to frontend and configure view controllers for SPA routing
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Route root path
        registry.addViewController("/")
                .setViewName("forward:/index.html");
        
        // Route frontend paths to index.html for SPA (adjust as needed for your routes)
        registry.addViewController("/login")
                .setViewName("forward:/index.html");
        registry.addViewController("/dashboard")
                .setViewName("forward:/index.html");
        registry.addViewController("/posts")
                .setViewName("forward:/index.html");
        registry.addViewController("/success")
                .setViewName("forward:/index.html");
        registry.addViewController("/failed")
                .setViewName("forward:/index.html");
        registry.addViewController("/auth/callback/**")
                .setViewName("forward:/index.html");
    }
}
