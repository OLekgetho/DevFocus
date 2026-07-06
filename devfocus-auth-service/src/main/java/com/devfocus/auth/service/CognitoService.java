package com.devfocus.auth.service;

import com.devfocus.shared.constants.ErrorCode;
import com.devfocus.shared.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CognitoService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;
    private final String clientId;
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*";
    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SYMBOLS;


    public CognitoService(CognitoIdentityProviderClient cognitoClient,
                          @Value("${aws.cognito.user-pool-id}") String userPoolId,
                          @Value("${aws.cognito.client-id}") String clientId) {
        this.cognitoClient = cognitoClient;
        this.userPoolId = userPoolId;
        this.clientId = clientId;
    }

    public boolean userExists(String username) {

        try {
            cognitoClient.adminGetUser(AdminGetUserRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build());

            return true;

        } catch (UserNotFoundException e) {
            return false;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Cognito user lookup failed");
        }
    }

    public String getUser(String username) {

        try {
            AdminGetUserResponse response = cognitoClient.adminGetUser(AdminGetUserRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build());

            return response.userAttributes()
                    .stream().filter(attr -> "sub".equals(attr.name()))
                    .map(AttributeType::value)
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                            "Cognito sub attribute not found"));

        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Cognito user lookup failed");
        }
    }

    public String createUser(String username, String email) {
        try {
            AdminCreateUserResponse response = cognitoClient.adminCreateUser(AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .messageAction(MessageActionType.SUPPRESS)
                    .userAttributes(
                            AttributeType.builder().name("email").value(email).build(),
                            AttributeType.builder().name("email_verified").value("true").build())
                    .build()
            );

            return response.user().attributes()
                    .stream().filter(attr -> "sub".equals(attr.name()))
                    .map(AttributeType::value)
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                            "Cognito sub attribute not found"));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cognito user creation failed");
        }
    }

    private void setUserPassword(String username, String password) {

        try {
            cognitoClient.adminSetUserPassword(AdminSetUserPasswordRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username).password(password)
                    .permanent(true)
                    .build());

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cognito set password failed");
        }
    }

    private AdminInitiateAuthResponse issueTokens(String username, String password) {

        try {
            return cognitoClient.adminInitiateAuth(AdminInitiateAuthRequest
                    .builder()
                    .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .authParameters(Map.of(
                            "USERNAME",username,
                            "PASSWORD",password
                            )
                    ).build()
            );

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cognito authentication failed");
        }
    }

    public AdminInitiateAuthResponse refreshToken(String refreshToken) {
        try {
            return cognitoClient.adminInitiateAuth(AdminInitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(clientId).userPoolId(userPoolId)
                    .authParameters(Map.of("REFRESH_TOKEN", refreshToken)).build());

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cognito token refresh failed");
        }
    }

    public void logout(String username) {

        try {
            cognitoClient.adminUserGlobalSignOut(AdminUserGlobalSignOutRequest
                    .builder()
                    .username(username).userPoolId(userPoolId)
                    .build());

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cognito logout failed");
        }
    }

    public void deleteUser(String username) {

        try {
            cognitoClient.adminDeleteUser(AdminDeleteUserRequest
                    .builder().username(username).userPoolId(userPoolId).build());

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cognito user delete failed");
        }
    }

    private String passwordGenerator() {
        SecureRandom secureRandom = new SecureRandom();

        int randomUpperCaseIndex = secureRandom.nextInt(UPPERCASE.length());
        int randomLowerCaseIndex = secureRandom.nextInt(LOWERCASE.length());
        int randomDigitsIndex = secureRandom.nextInt(DIGITS.length());
        int randomSymbolIndex = secureRandom.nextInt(SYMBOLS.length());

        char randomUpperCaseChar = UPPERCASE.charAt(randomUpperCaseIndex);
        char randomLowerCaseChar = LOWERCASE.charAt(randomLowerCaseIndex);
        char randomDigitsChar = DIGITS.charAt(randomDigitsIndex);
        char randomSymbolChar = SYMBOLS.charAt(randomSymbolIndex);

        List<Character> passwordChar = new ArrayList<>();

        passwordChar.add(randomUpperCaseChar);
        passwordChar.add(randomLowerCaseChar);
        passwordChar.add(randomDigitsChar);
        passwordChar.add(randomSymbolChar);

        int totalLength = 20;

        while (passwordChar.size() < totalLength) {
            int randomString = secureRandom.nextInt(ALL_CHARACTERS.length());
            char character = ALL_CHARACTERS.charAt(randomString);
            passwordChar.add(character);
        }

        Collections.shuffle(passwordChar, secureRandom);

        StringBuilder stringBuilder = new StringBuilder();
        for (Character character : passwordChar) {
            stringBuilder.append(character);
        }

        return stringBuilder.toString();

    }

    public AdminInitiateAuthResponse authenticateAndIssueTokens(String username) {

        String password =  passwordGenerator();
        setUserPassword(username, password);
        return issueTokens(username, password);

    }
}
