package com.devfocus.auth.service;

import com.devfocus.auth.dto.*;
import com.devfocus.auth.entity.User;
import com.devfocus.auth.repository.UserRepository;
import com.devfocus.shared.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.assertj.core.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private GitHubOAuthService gitHubOAuthService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private CognitoService cognitoService;

    @InjectMocks
    private AuthServiceImpl authService;


    @Test
    void handleGitHubCallBack_returningUser_UpdatesTokenAndSkipCreation() {

        GitHubCallbackRequest request = new GitHubCallbackRequest("auth-code-123");

        GitHubTokenResponse fakeTokenResponse = new GitHubTokenResponse();
        fakeTokenResponse.setAccessToken("gho_12345");

        GitHubUserProfile fakeUserProfile = new GitHubUserProfile();
        fakeUserProfile.setId(999L);
        fakeUserProfile.setEmail("ofentse@devfocus.co.za");


        User existingUser =  new User();
        existingUser.setGithubId(999L);
        existingUser.setCognitoSub("cognito-uuid-xyc");


        AuthenticationResultType dummyAuthResult = AuthenticationResultType.builder()
                .idToken("dummy-id-token")
                .refreshToken("dummy-refresh-token")
                .build();

        AdminInitiateAuthResponse dummyAwsResponse = AdminInitiateAuthResponse.builder()
                .authenticationResult(dummyAuthResult)
                .build();

        when(gitHubOAuthService.exchangeCodeForToken("auth-code-123")).thenReturn(fakeTokenResponse);
        when(gitHubOAuthService.fetchUserProfile("gho_12345")).thenReturn(fakeUserProfile);
        when(encryptionService.encrypt("gho_12345")).thenReturn("encrypted_gho_12345");
        when(userRepository.findByGithubId(999L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(cognitoService.authenticateAndIssueTokens(anyString())).thenReturn(dummyAwsResponse);

        AuthResponse authResponse = authService.handleGitHubCallback(request);

        assertThat(authResponse).isNotNull();

        verify(userRepository).save(any(User.class));

        verify(gitHubOAuthService, never()).fetchPrimaryEmail(anyString());

        verify(cognitoService, never()).createUser(anyString(), anyString());

    }

    @Test
    void handleGitHubCallback_NewUserWithoutEmail_FetchesEmailAndCreatesUser() {
        GitHubCallbackRequest request = new GitHubCallbackRequest("auth-code-123");

        GitHubTokenResponse fakeTokenResponse = new GitHubTokenResponse();
        fakeTokenResponse.setAccessToken("gho_12345");

        GitHubUserProfile fakeUserProfile = new GitHubUserProfile();
        fakeUserProfile.setId(999L);
        fakeUserProfile.setEmail(null);

        User creationUser =  new User();
        creationUser.setGithubId(999L);
        creationUser.setCognitoSub("cognito-uuid-xyc");

        AuthenticationResultType dummyAuthResult = AuthenticationResultType.builder()
                .idToken("dummy-id-token")
                .refreshToken("dummy-refresh-token")
                .build();

        AdminInitiateAuthResponse dummyAwsResponse = AdminInitiateAuthResponse.builder()
                .authenticationResult(dummyAuthResult)
                .build();

        when(gitHubOAuthService.exchangeCodeForToken("auth-code-123")).thenReturn(fakeTokenResponse);
        when(gitHubOAuthService.fetchUserProfile("gho_12345")).thenReturn(fakeUserProfile);
        when(gitHubOAuthService.fetchPrimaryEmail("gho_12345")).thenReturn("fallback@devfocus.co.za");
        when(encryptionService.encrypt("gho_12345")).thenReturn("encrypted_gho_12345");
        when(userRepository.findByGithubId(999L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(creationUser);
        when(cognitoService.createUser(anyString(), anyString())).thenReturn("new-cognito-sub");
        when(cognitoService.authenticateAndIssueTokens(anyString())).thenReturn(dummyAwsResponse);

        AuthResponse authResponse = authService.handleGitHubCallback(request);

        assertThat(authResponse).isNotNull();

        verify(gitHubOAuthService, times(1)).fetchPrimaryEmail(anyString());

        verify(cognitoService, times(1)).createUser(anyString(), anyString());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void logoutTranslatesSubToGithubIdAndLogsOut() {
        User user = new User();
        user.setGithubId(888L);
        user.setCognitoSub("cognito-123");

        when(userRepository.findByCognitoSub("cognito-123")).thenReturn(Optional.of(user));

        authService.logout("cognito-123");

        verify(cognitoService, times(1)).logout("888");
    }

    @Test
    void logoutThrowsAppExceptionWhenUserNotFound() {
        when(userRepository.findByCognitoSub("invalid-sub")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("invalid-sub"))
                .isInstanceOf(AppException.class)
                .hasMessage("User not found");

        verify(cognitoService, never()).logout(anyString());
    }

    @Test
    void refreshTokenReturnsNewIdToken() {

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("new-refresh-token");

        AuthenticationResultType dummyAuthResult = AuthenticationResultType.builder()
                .idToken("dummy-id-token")
                .refreshToken("new-id-token")
                .build();

        AdminInitiateAuthResponse dummyAwsResponse = AdminInitiateAuthResponse.builder()
                .authenticationResult(dummyAuthResult)
                .build();

        when(cognitoService.refreshToken(anyString())).thenReturn(dummyAwsResponse);

        TokenResponse tokenResponse = authService.refreshToken(refreshTokenRequest);

        assertThat(tokenResponse).isNotNull();

        assertThat(tokenResponse.getIdToken()).isEqualTo("dummy-id-token");
        verify(cognitoService, times(1)).refreshToken("new-refresh-token");
    }

    @Test
    void disconnectGitHubSetsTokenToNullAndSaves() {
        User user = new User();
        user.setGithubAccessToken("encrypted-secret");
        user.setCognitoSub("cognito-123");
        user.setGithubId(882L);

        when(userRepository.findByCognitoSub("cognito-123")).thenReturn(Optional.of(user));

        authService.disconnectGitHub("cognito-123");

        assertThat(user.getGithubAccessToken()).isNull();

        verify(userRepository, times(1)).findByCognitoSub("cognito-123");
        verify(userRepository, times(1)).save(user);

    }

    @Test
    void disconnectGitHubThrowsWhenUserNotFound() {

        when(userRepository.findByCognitoSub("cognito-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.disconnectGitHub("cognito-123"))
                .isInstanceOf(AppException.class)
                .hasMessage("User not found");

    }

    @Test
    void connectGitHubUpdatesUserAndReturnsResponse() {
        GitHubConnectRequest gitHubConnectRequest = new GitHubConnectRequest();
        gitHubConnectRequest.setCode("dummy-code");

        GitHubTokenResponse gitHubTokenResponse = new GitHubTokenResponse();
        gitHubTokenResponse.setAccessToken("dummy-accessToken");

        GitHubUserProfile gitHubUserProfile = new GitHubUserProfile();
        gitHubUserProfile.setId(999L);
        gitHubUserProfile.setEmail("fallback.@devfocus.co.za");

        User user = new User();
        user.setCognitoSub("cognito-123");
        user.setGithubId(999L);
        user.setGithubAccessToken("encrypted-secret");

        when(gitHubOAuthService.exchangeCodeForToken("dummy-code")).thenReturn(gitHubTokenResponse);
        when(gitHubOAuthService.fetchUserProfile("dummy-accessToken")).thenReturn(gitHubUserProfile);
        when(userRepository.findByCognitoSub("cognito-123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(encryptionService.encrypt("dummy-accessToken")).thenReturn("encrypted-secret");

        authService.connectGitHub(gitHubConnectRequest, "cognito-123");

        assertThat(user.getGithubAccessToken()).isEqualTo("encrypted-secret");
        assertThat(user.getCognitoSub()).isEqualTo("cognito-123");

        verify(userRepository, times(1)).findByCognitoSub("cognito-123");
        verify(userRepository, times(1)).save(user);


    }

}
