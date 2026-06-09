package com.devfocus.shared.constants;

public final class AppConstants {

    private AppConstants() {

    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String COGNITO_SUB_CLAIM = "sub";
    public static final String GITHUB_TOKEN_HEADER = "X-GitHub-Token";
    public static final String USER_CONTEXT_HEADER = "X-User-Context";

    public static final int GITHUB_API_TIMEOUT_SECONDS = 10;
    public static final int JWT_EXPIRY_HOURS = 1;
    public static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
}
