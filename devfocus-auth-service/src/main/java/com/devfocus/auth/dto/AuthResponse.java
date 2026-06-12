package com.devfocus.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {

    private String idToken;
    private String refreshToken;
    private String username;
    private String avatarUrl;
    private String email;
    private boolean isFirstLogin;

}
