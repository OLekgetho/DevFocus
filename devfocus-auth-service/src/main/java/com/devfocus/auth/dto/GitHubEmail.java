package com.devfocus.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GitHubEmail {
    private String email;
    private boolean primary;
    private boolean verified;
}
