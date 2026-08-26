package com.devfocus.auth.service;

import com.devfocus.auth.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private AuthService authService;
}
