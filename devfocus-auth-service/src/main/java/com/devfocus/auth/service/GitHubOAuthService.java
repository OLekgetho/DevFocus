package com.devfocus.auth.service;

import com.devfocus.auth.dto.GitHubEmail;
import com.devfocus.auth.dto.GitHubTokenResponse;
import com.devfocus.auth.dto.GitHubUserProfile;
import com.devfocus.shared.constants.AppConstants;
import com.devfocus.shared.constants.ErrorCode;
import com.devfocus.shared.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class GitHubOAuthService {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final WebClient gitHubWebClient;
    private final WebClient oauthClient;

    public GitHubOAuthService(@Value("${github.client-id}")String clientId,
                              @Value("${github.client-secret}")String clientSecret,
                              @Value("${github.redirect-uri}")String redirectUri,
                              WebClient gitHubWebClient,
                              WebClient oauthClient) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.gitHubWebClient = gitHubWebClient;
        this.oauthClient = oauthClient;
    }

    public String buildAuthorizationUrl() {

        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user user:email repo")
                .toUriString();
    }

    public GitHubTokenResponse exchangeCodeForToken(String code) {

        try {
            GitHubTokenResponse githubTokenResponse = oauthClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/login/oauth/access_token")
                            .queryParam("client_id", clientId)
                            .queryParam("client_secret", clientSecret)
                            .queryParam("code", code)
                            .queryParam("redirect_uri", redirectUri)
                            .build()
                    ).retrieve()
            .bodyToMono(GitHubTokenResponse.class)
            .block();

            if (githubTokenResponse == null || githubTokenResponse.getAccessToken() == null) {
                throw new AppException(ErrorCode.GITHUB_API_ERROR, HttpStatus.BAD_GATEWAY, "GitHub token exchange failed");
            }

            return githubTokenResponse;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.GITHUB_API_ERROR, HttpStatus.BAD_GATEWAY, "GitHub token exchange failed");
        }
    }

    public GitHubUserProfile fetchUserProfile(String accessToken) {

        try {
            GitHubUserProfile gitHubUserProfile = gitHubWebClient.get()
                    .uri("/user")
                    .header(HttpHeaders.AUTHORIZATION, AppConstants.BEARER_PREFIX + accessToken)
                    .retrieve()
                    .bodyToMono(GitHubUserProfile.class)
                    .block();

            if (gitHubUserProfile == null ) {
                throw new AppException(ErrorCode.GITHUB_API_ERROR,
                        HttpStatus.BAD_GATEWAY,
                        "Failed to fetch Github user profile");
            }
            return gitHubUserProfile;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.GITHUB_API_ERROR,
                    HttpStatus.BAD_GATEWAY,
                    "Failed to fetch Github user profile");
        }
    }

    public String fetchPrimaryEmail(String accessToken) {

        try {
            List<GitHubEmail> gitHubEmails = gitHubWebClient.get()
                    .uri("/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, AppConstants.BEARER_PREFIX + accessToken)
                    .retrieve()
                    .bodyToFlux(GitHubEmail.class)
                    .collectList()
                    .block();

            if (gitHubEmails == null || gitHubEmails.isEmpty()) {
                throw new AppException(ErrorCode.GITHUB_API_ERROR,
                        HttpStatus.BAD_GATEWAY,
                        "Failed to fetch Github user email");
            }

            return getGitHubEmail(gitHubEmails);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.GITHUB_API_ERROR,
                    HttpStatus.BAD_GATEWAY,
                    "No verified primary email available from GitHub");
        }
    }

    private String getGitHubEmail(List<GitHubEmail> gitHubEmails) {
        return gitHubEmails.stream()
                .filter(e -> e.isPrimary() && e.isVerified())
                .map(GitHubEmail::getEmail)
                .findFirst()
                .orElseThrow(() -> new AppException(
                        ErrorCode.GITHUB_API_ERROR,
                        HttpStatus.BAD_GATEWAY,
                        "No verified primary email available from GitHub"
                ));
    }
}
