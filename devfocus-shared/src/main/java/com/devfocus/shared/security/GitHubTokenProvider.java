package com.devfocus.shared.security;

import com.devfocus.shared.constants.ErrorCode;
import com.devfocus.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GitHubTokenProvider {

    public String getToken(String cognitoSub) {

        // TODO: Query devfocus_auth schema to retrieve and decrypt the GitHub access token for this user
        //  Full implementation will be done when devfocus-auth-service is built

        throw new AppException(ErrorCode.GITHUB_TOKEN_EXPIRED,
                HttpStatus.UNAUTHORIZED,
                "GitHubTokenProvider not yet implemented");
    }
}
