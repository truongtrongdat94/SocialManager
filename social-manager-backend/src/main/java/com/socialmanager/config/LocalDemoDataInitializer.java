package com.socialmanager.config;

import com.socialmanager.model.Platform;
import com.socialmanager.util.EncryptionUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoDataInitializer implements ApplicationRunner {

    public static final UUID DEMO_SOCIAL_ACCOUNT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionUtil encryptionUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureLocalSchema();
        UUID userId = ensureUser();
        ensureSocialAccount(userId);
    }

    private void ensureLocalSchema() {
        jdbcTemplate.execute("""
                create table if not exists users (
                    id uuid primary key,
                    email varchar(255) not null unique,
                    password varchar(255),
                    name varchar(255),
                    google_id varchar(255),
                    created_at timestamp,
                    updated_at timestamp
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists social_accounts (
                    id uuid primary key,
                    user_id uuid not null,
                    platform varchar(32) not null,
                    external_account_id varchar(255),
                    account_alias varchar(255),
                    account_name varchar(255),
                    profile_picture_url clob,
                    access_token clob,
                    refresh_token clob,
                    expires_at timestamp,
                    scopes clob,
                    is_auto_pilot boolean,
                    created_at timestamp,
                    constraint fk_social_accounts_user foreign key (user_id) references users(id)
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists scheduled_posts (
                    id uuid primary key,
                    user_id uuid not null,
                    social_account_id uuid not null,
                    caption clob,
                    media_urls varchar array,
                    scheduled_time timestamp,
                    status varchar(32) not null,
                    published_post_id varchar(255),
                    error_message clob,
                    retry_count integer not null,
                    last_attempt_at timestamp,
                    is_auto_pilot boolean,
                    created_at timestamp,
                    constraint fk_scheduled_posts_user foreign key (user_id) references users(id),
                    constraint fk_scheduled_posts_social_account foreign key (social_account_id) references social_accounts(id)
                )
                """);
    }

    private UUID ensureUser() {
        var existing = entityManager.createQuery(
                "select u from User u where u.email = :email", com.socialmanager.model.User.class)
            .setParameter("email", "devuser")
                .getResultStream()
                .findFirst();

        if (existing.isPresent()) {
            var user = existing.get();
            boolean changed = false;

            if (user.getUsername() == null || user.getUsername().isBlank()) {
                user.setUsername("devuser");
                changed = true;
            }

            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode("devpass123"));
                changed = true;
            }

            if (changed) {
                entityManager.merge(user);
            }

            return user.getId();
        }

        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        entityManager.createNativeQuery("""
                insert into users (id, email, username, password, name, created_at, updated_at)
                values (:id, :email, :username, :password, :name, current_timestamp, current_timestamp)
                """)
                .setParameter("id", userId)
            .setParameter("email", "devuser")
                .setParameter("username", "devuser")
                .setParameter("password", passwordEncoder.encode("devpass123"))
                .setParameter("name", "Local Demo User")
                .executeUpdate();
        return userId;
    }

    private void ensureSocialAccount(UUID userId) {
        boolean exists = !entityManager.createQuery(
                        "select sa.id from SocialAccount sa where sa.id = :id", UUID.class)
                .setParameter("id", DEMO_SOCIAL_ACCOUNT_ID)
                .getResultList()
                .isEmpty();

        if (exists) {
            return;
        }

        String encryptedToken = encryptionUtil.encrypt("local-demo-token");
        entityManager.createNativeQuery("""
                insert into social_accounts (
                    id, user_id, platform, external_account_id, account_alias, account_name,
                    access_token, is_auto_pilot, created_at
                ) values (
                    :id, :userId, :platform, :externalAccountId, :accountAlias, :accountName,
                    :accessToken, true, current_timestamp
                )
                """)
                .setParameter("id", DEMO_SOCIAL_ACCOUNT_ID)
                .setParameter("userId", userId)
                .setParameter("platform", Platform.FACEBOOK.name())
                .setParameter("externalAccountId", "local-demo-page")
                .setParameter("accountAlias", "local-facebook")
                .setParameter("accountName", "Local Facebook Demo")
                .setParameter("accessToken", encryptedToken)
                .executeUpdate();
    }
}