package com.socialmanager.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to default OidcUserService to get the OidcUser with all claims
        OidcUser oidcUser = delegate.loadUser(userRequest);
        // email, name, sub (googleId) are available via oidcUser.getEmail(),
        // oidcUser.getFullName(), oidcUser.getSubject()
        // Persistence is handled by OAuth2AuthenticationSuccessHandler
        return oidcUser;
    }
}
