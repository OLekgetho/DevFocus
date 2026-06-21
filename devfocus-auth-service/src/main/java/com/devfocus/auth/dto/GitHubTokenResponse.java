package com.devfocus.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GitHubTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;
}
