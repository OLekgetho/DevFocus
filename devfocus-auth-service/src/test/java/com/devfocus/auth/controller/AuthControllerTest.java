package com.devfocus.auth.controller;

import com.devfocus.auth.dto.AuthResponse;
import com.devfocus.auth.dto.GitHubCallbackRequest;
import com.devfocus.auth.dto.RefreshTokenRequest;
import com.devfocus.auth.dto.TokenResponse;
import com.devfocus.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;


    @Test
    void handleGitHubCallback_Returns200_WhenValidRequest() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .idToken("cool-id-token")
                .username("ofentse")
                .refreshToken("wow")
                .email("test-ofentse@devfocus.co.za")
                .build();

        when(authService.handleGitHubCallback(any(GitHubCallbackRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/github/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"code\": \"12345\" }")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idToken").value("cool-id-token"));
    }

    @Test
    void refreshToken_Returns200_WhenValidRequest() throws Exception {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setIdToken("cool-id-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"refreshToken\": \"sasasd12345\" }")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idToken").value("cool-id-token"));
    }
}
