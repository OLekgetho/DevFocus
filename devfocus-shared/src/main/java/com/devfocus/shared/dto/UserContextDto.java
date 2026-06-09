package com.devfocus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserContextDto {

    private String cognitoSub;
    private Long githubId;
    private String username;
    private String avatarUrl;
    private String email;
}
