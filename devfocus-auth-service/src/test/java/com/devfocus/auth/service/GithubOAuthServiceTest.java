package com.devfocus.auth.service;

import com.devfocus.auth.dto.GitHubTokenResponse;
import com.devfocus.auth.dto.GitHubUserProfile;
import com.devfocus.shared.exception.AppException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

public class GithubOAuthServiceTest {
    private MockWebServer mockWebServer;
    private GitHubOAuthService gitHubOAuthService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient testWebClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        gitHubOAuthService = new GitHubOAuthService(
                "test-client-id",
                "test-client-secret",
                "http://localhost:8081/callback",
                testWebClient,   // gitHubWebClient
                testWebClient    // oauthClient — same fake server serves both in these tests
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }


    @Test
    void exchangeCodeForTokenReturnsAccessToken() throws InterruptedException {
        String fakeResponseJson = """
                {"access_token": "gho_fakeToken123"}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(fakeResponseJson)
                .addHeader("Content-Type", "application/json"));

        GitHubTokenResponse result = gitHubOAuthService.exchangeCodeForToken("one-piece");

        assertThat(result.getAccessToken()).isEqualTo("gho_fakeToken123");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }

    @Test
    void fetchUserProfileReturnsUserProfile() throws InterruptedException {
        String fakeUserPorfileResponseJson = """
                {"id": "2323423",
                "login": "oansnie",
                "avatar_url": "http://avatar_url.com",
                "email": "oansnie@gmail.com"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(fakeUserPorfileResponseJson)
                .addHeader("Content-Type", "application/json"));

        GitHubUserProfile userProfileResult = gitHubOAuthService.fetchUserProfile("lufffffy");

        assertThat(userProfileResult.getEmail()).isEqualTo("oansnie@gmail.com");
        assertThat(userProfileResult.getLogin()).isEqualTo("oansnie");
        assertThat(userProfileResult.getId()).isEqualTo(2323423);
        assertThat(userProfileResult.getAvatarUrl()).isEqualTo("http://avatar_url.com");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
    }

    @Test
    void fetchPrimaryEmailReturnsPrimaryEmail() throws InterruptedException {
        String fakePrimaryEmailResponseJson = """
        [
          {"email": "secondary@gmail.com", "primary": false, "verified": true},
          {"email": "oansnie@gmail.com", "primary": true, "verified": true}
        ]
        """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(fakePrimaryEmailResponseJson)
                .addHeader("Content-Type", "application/json"));

        String primaryEmail =  gitHubOAuthService.fetchPrimaryEmail("lufffffy");

        assertThat(primaryEmail).isEqualTo("oansnie@gmail.com");

        RecordedRequest  recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
    }

    @Test
    void fetchPrimaryEmailReturnsError() throws InterruptedException {
        String fakePrimaryEmailResponseJson = """
        [
          {"email": "secondary@gmail.com", "primary": false, "verified": true},
          {"email": "oansnie@gmail.com", "primary": true, "verified": false}
        ]
        """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(fakePrimaryEmailResponseJson)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> gitHubOAuthService.fetchPrimaryEmail("luffyyy"))
                .isInstanceOf(AppException.class);
    }
}
