package com.devfocus.auth.service;

import com.devfocus.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@ExtendWith(MockitoExtension.class)
public class CognitoServiceTest {

    @Mock
    private CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private CognitoService cognitoService;
    
    @BeforeEach
    void setUp() {
        cognitoService = new CognitoService(cognitoIdentityProviderClient,
                "test-pool-id",
                "test-client-id");
    }

    @Test
    void userExistsReturnsTrueWhenUserFound() {
        when(cognitoIdentityProviderClient.adminGetUser(any(AdminGetUserRequest.class)))
                .thenReturn(AdminGetUserResponse.builder().build());

        boolean result = cognitoService.userExists("Luffy");
        assertThat(result).isTrue();
    }

    @Test
    void userExistsReturnsFalseWhenUserNotFound() {
        when(cognitoIdentityProviderClient.adminGetUser(any(AdminGetUserRequest.class)))
                .thenThrow(UserNotFoundException.builder().build());

        boolean result = cognitoService.userExists("Luffy");
        assertThat(result).isFalse();
    }

    @Test
    void createUserReturnsSubSuccessfully() {
        AttributeType subAttribute = AttributeType.builder()
                .name("sub")
                .value("the-sunny-uuid")
                .build();

        UserType mockUser = UserType.builder()
                .attributes(subAttribute)
                .build();

        AdminCreateUserResponse mockResponse = AdminCreateUserResponse.builder()
                .user(mockUser)
                .build();

        when(cognitoIdentityProviderClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenReturn(mockResponse);

        String userResponse = cognitoService.createUser(
                "test", "testEmail@example.com");

        assertThat(userResponse).isEqualTo("the-sunny-uuid");
    }

    @Test
    void setUserPasswordSucceeds() {
        when(cognitoIdentityProviderClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class)))
                .thenReturn(AdminSetUserPasswordResponse.builder().build());

        assertThatCode(() -> cognitoService.setUserPassword("luffy", "22331"))
                .doesNotThrowAnyException();
    }

    @Test
    void setUserPasswordThrowsAppExceptionOnFailure() {
        when(cognitoIdentityProviderClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class)))
                .thenThrow(CognitoIdentityProviderException.builder().message("AWS failed").build());

        assertThatThrownBy(() -> cognitoService.setUserPassword("user", "newPass123!"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cognito set password failed");
    }

    @Test
    void logoutCallsAwsWithCorrectUsername() {
        when(cognitoIdentityProviderClient.adminUserGlobalSignOut(any(AdminUserGlobalSignOutRequest.class)))
                .thenReturn(AdminUserGlobalSignOutResponse.builder().build());

        String targetUsername = "luffy_101";
        cognitoService.logout(targetUsername);

        ArgumentCaptor<AdminUserGlobalSignOutRequest> captor = ArgumentCaptor.forClass(AdminUserGlobalSignOutRequest.class);

        verify(cognitoIdentityProviderClient).adminUserGlobalSignOut(captor.capture());

        AdminUserGlobalSignOutRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.username()).isEqualTo(targetUsername);
        assertThat(capturedRequest.userPoolId()).isEqualTo("test-pool-id"); // Bonus: verify our config was used!
    }

    @Test
    void authenticateAndIssueTokensSucceeds() {
        when(cognitoIdentityProviderClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class)))
                .thenReturn(AdminSetUserPasswordResponse.builder().build());

        when(cognitoIdentityProviderClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class)))
                .thenReturn(AdminInitiateAuthResponse.builder().build());

        String testUser = "github_user_123";
        cognitoService.authenticateAndIssueTokens(testUser);

        ArgumentCaptor<AdminSetUserPasswordRequest> setPasswordCaptor = ArgumentCaptor.forClass(AdminSetUserPasswordRequest.class);
        ArgumentCaptor<AdminInitiateAuthRequest> initiateAuthCaptor = ArgumentCaptor.forClass(AdminInitiateAuthRequest.class);

        verify(cognitoIdentityProviderClient).adminSetUserPassword(setPasswordCaptor.capture());
        verify(cognitoIdentityProviderClient).adminInitiateAuth(initiateAuthCaptor.capture());

        assertThat(setPasswordCaptor.getValue().username()).isEqualTo(initiateAuthCaptor.getValue().authParameters().get("USERNAME"));
        assertThat(setPasswordCaptor.getValue().password()).isEqualTo(initiateAuthCaptor.getValue().authParameters().get("PASSWORD"));
    }
}
