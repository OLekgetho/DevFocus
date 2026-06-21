package com.devfocus.auth.entity;

import com.devfocus.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "cognito_sub", unique = true, nullable = false)
    private String cognitoSub;

    @Column(name = "github_id", unique = true, nullable = false)
    private Long githubId;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(name = "github_access_token", columnDefinition = "TEXT")
    private String githubAccessToken;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String email;

    @Builder.Default
    @Column(name = "is_first_login")
    private boolean isFirstLogin = true;


    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
