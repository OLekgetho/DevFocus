package com.devfocus.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GitHubUserResponse {

    private String username;
    private String avatarUrl;
    private String email;
}
