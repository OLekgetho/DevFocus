package com.devfocus.auth.service;

import com.devfocus.auth.dto.*;
import com.devfocus.auth.entity.User;
import com.devfocus.auth.repository.UserRepository;
import com.devfocus.shared.constants.ErrorCode;
import com.devfocus.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final CognitoService cognitoService;
    private final EncryptionService encryptionService;
    private final GitHubOAuthService gitHubOAuthService;
    private final UserRepository userRepository;


    public AuthServiceImpl(CognitoService cognitoService,
                           EncryptionService encryptionService,
                           GitHubOAuthService gitHubOAuthService,
                           UserRepository userRepository) {
        this.cognitoService = cognitoService;
        this.encryptionService = encryptionService;
        this.gitHubOAuthService = gitHubOAuthService;
        this.userRepository = userRepository;
    }

    @Override
    public String getGitHubAuthUrl() {
        return gitHubOAuthService.buildAuthorizationUrl();
    }

    @Override
    @Transactional
    public AuthResponse handleGitHubCallback(GitHubCallbackRequest request) {
        GitHubTokenResponse githubTokenResponse = gitHubOAuthService.exchangeCodeForToken(
                request.getCode()
        );

        GitHubUserProfile gitHubUserProfile = gitHubOAuthService.fetchUserProfile(
                githubTokenResponse.getAccessToken()
        );

        String email = gitHubUserProfile.getEmail();

        if (email == null || email.isBlank()) {
            email = gitHubOAuthService.fetchPrimaryEmail(githubTokenResponse.getAccessToken());
        }

        String encryptedToken =  encryptionService.encrypt(githubTokenResponse.getAccessToken());

        Optional<User> dbUserExists = userRepository.findByGithubId(gitHubUserProfile.getId());

        User user = dbUserExists.isEmpty()
                ? createNewUser(gitHubUserProfile, encryptedToken, email)
                : updateExistingUser(dbUserExists.get(), encryptedToken);

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        AdminInitiateAuthResponse adminInitiateAuthResponse =
                cognitoService.authenticateAndIssueTokens(user.getGithubId().toString());

        AuthenticationResultType authenticationResult = adminInitiateAuthResponse.authenticationResult();

        String idToken = authenticationResult.idToken();
        String refreshToken = authenticationResult.refreshToken();

        return AuthResponse
                .builder()
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .isFirstLogin(user.isFirstLogin())
                .idToken(idToken)
                .refreshToken(refreshToken)
                .username(user.getGithubUsername())
                .build();
    }

    private User updateExistingUser(User dbUser, String encryptedToken) {

        dbUser.setLastSeenAt(Instant.now());
        dbUser.setGithubAccessToken(encryptedToken);
        dbUser.setFirstLogin(false);

        return userRepository.save(dbUser);

    }

    private User createNewUser(GitHubUserProfile gitHubUserProfile, String encryptedToken, String email) {
        boolean cognitoUserExists = cognitoService.userExists(gitHubUserProfile.getId().toString());

        String userCognitoSub =  !cognitoUserExists ?
                cognitoService.createUser(gitHubUserProfile.getId().toString(), email) :
                cognitoService.getUser(gitHubUserProfile.getId().toString());

        User newUser = User.builder()
                .avatarUrl(gitHubUserProfile.getAvatarUrl())
                .email(email)
                .githubAccessToken(encryptedToken)
                .lastSeenAt(Instant.now())
                .cognitoSub(userCognitoSub)
                .githubUsername(gitHubUserProfile.getLogin())
                .githubId(gitHubUserProfile.getId())
                .build();

        return userRepository.save(newUser);

    }


    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {

        AdminInitiateAuthResponse authResponse = cognitoService.refreshToken(
                request.getRefreshToken());

        AuthenticationResultType authenticationResult = authResponse.authenticationResult();

        return TokenResponse.builder()
                .idToken(authenticationResult.idToken())
                .build();
    }

    @Override
    public void logout(String cognitoSub) {
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
        cognitoService.logout(user.getGithubId().toString());
    }

    @Override
    @Transactional
    public GitHubUserResponse connectGitHub(GitHubConnectRequest request, String cognitoSub) {
        GitHubTokenResponse githubTokenResponse = gitHubOAuthService.exchangeCodeForToken(
                request.getCode()
        );

        GitHubUserProfile gitHubUserProfile = gitHubOAuthService.fetchUserProfile(
                githubTokenResponse.getAccessToken()
        );

        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));

        String encryptedToken =  encryptionService.encrypt(githubTokenResponse.getAccessToken());

        user.setGithubAccessToken(encryptedToken);
        user.setAvatarUrl(gitHubUserProfile.getAvatarUrl());
        user.setEmail(gitHubUserProfile.getEmail());
        user.setGithubUsername(gitHubUserProfile.getLogin());
        user.setGithubId(gitHubUserProfile.getId());
        userRepository.save(user);


        return GitHubUserResponse
                .builder()
                .avatarUrl(gitHubUserProfile.getAvatarUrl())
                .email(gitHubUserProfile.getEmail())
                .username(gitHubUserProfile.getLogin())
                .build();
    }

    @Transactional
    @Override
    public void disconnectGitHub(String cognitoSub) {
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));

        user.setGithubAccessToken(null);
        userRepository.save(user);
    }
}
