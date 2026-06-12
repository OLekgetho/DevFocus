package com.devfocus.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GitHubUserProfile {

    private Long id;

    private String login;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private String email;

}
