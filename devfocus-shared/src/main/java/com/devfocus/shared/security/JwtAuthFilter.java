package com.devfocus.shared.security;

import com.devfocus.shared.constants.ErrorCode;
import com.devfocus.shared.exception.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${aws.cognito.jwk-set-uri}")
    private String cognitoJwkSetUri;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {

            filterChain.doFilter(request, response);
            return;
        }

        String authToken = authHeader.substring(BEARER_PREFIX.length());

        try {
            Jwt jwt = NimbusJwtDecoder.withJwkSetUri(cognitoJwkSetUri)
                    .build().decode(authToken);

            String cognitoSub = jwt.getClaim("sub");

            UserPrincipal userPrincipal = new UserPrincipal(cognitoSub);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                         userPrincipal,
                         null,
                         userPrincipal.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {

            throw new AppException(
                    ErrorCode.AUTH_TOKEN_INVALID,
                    HttpStatus.UNAUTHORIZED,
                    ex.getMessage()
            );
        }

    }
}
