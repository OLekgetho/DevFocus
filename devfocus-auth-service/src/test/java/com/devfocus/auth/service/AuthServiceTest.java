package com.devfocus.auth.service;

import com.devfocus.auth.dto.AuthResponse;
import com.devfocus.auth.dto.GitHubCallbackRequest;
import com.devfocus.auth.dto.GitHubTokenResponse;
import com.devfocus.auth.dto.GitHubUserProfile;
import com.devfocus.auth.entity.User;
import com.devfocus.auth.repository.UserRepository;
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
}
