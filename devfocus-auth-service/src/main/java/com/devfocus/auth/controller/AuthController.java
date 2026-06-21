package com.devfocus.auth.controller;

import com.devfocus.auth.dto.*;
import com.devfocus.auth.service.AuthService;
import com.devfocus.shared.response.ApiResponse;
import com.devfocus.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/github/url")
    public ResponseEntity<ApiResponse<String>> getGitHubAuthUrl(HttpServletRequest httpServletRequest) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "GitHub authorization URL generated",
                        authService.getGitHubAuthUrl(),
                        httpServletRequest.getRequestURI()
                )
        );
    }

    @PostMapping("/github/callback")
    public ResponseEntity<ApiResponse<AuthResponse>> handleGitHubCallback(
             @Valid @RequestBody GitHubCallbackRequest request,
             HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                       "Login successful",
                       authService.handleGitHubCallback(request),
                       httpServletRequest.getRequestURI()
                )
        );
    }


    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Token refreshed",
                        authService.refreshToken(request),
                        httpServletRequest.getRequestURI()
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            HttpServletRequest httpServletRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        authService.logout(userPrincipal.getCognitoSub());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logged out successfully",
                        httpServletRequest.getRequestURI()
                )
        );
    }

    @PostMapping("/github/connect")
    public ResponseEntity<ApiResponse<GitHubUserResponse>> connectGitHub(
            @Valid @RequestBody GitHubConnectRequest request,
            HttpServletRequest httpServletRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "GitHub account connected",
                        authService.connectGitHub(request, userPrincipal.getCognitoSub()),
                        httpServletRequest.getRequestURI()
                )
        );
    }

    @DeleteMapping("/github/disconnect")
    public ResponseEntity<ApiResponse<?>> disconnectGitHub(
            HttpServletRequest httpServletRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        authService.disconnectGitHub(userPrincipal.getCognitoSub());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "GitHub account disconnected",
                        httpServletRequest.getRequestURI()
                )
        );
    }
}
