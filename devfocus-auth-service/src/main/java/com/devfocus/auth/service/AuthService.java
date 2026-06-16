package com.devfocus.auth.service;

import com.devfocus.auth.dto.*;

public interface AuthService {

    String getGitHubAuthUrl();

    AuthResponse handleGitHubCallback(GitHubCallbackRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(String cognitoSub);

    GitHubUserResponse connectGitHub(GitHubConnectRequest request, String cognitoSub);

    void disconnectGitHub(String cognitoSub);

}
