package com.devfocus.shared.response;

import com.devfocus.shared.constants.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;
    private String path;
    private ErrorCode errorCode;

    public static <T> ApiResponse<T> success(String message, T data, String path) {

        return ApiResponse.<T>builder().timestamp(Instant.now()).success(true)
                .data(data).message(message).errorCode(null).path(path).build();
    }

    public static <T> ApiResponse<T> success(String message, String path) {

        return ApiResponse.<T>builder().success(true).message(message).timestamp(Instant.now()).path(path)
                .errorCode(null).build();
    }

    public static <T> ApiResponse<T> error(String message, ErrorCode errorCode, String path) {

        return ApiResponse.<T>builder().success(false).message(message).data(null).timestamp(Instant.now())
                .path(path).errorCode(errorCode).build();
    }

}
