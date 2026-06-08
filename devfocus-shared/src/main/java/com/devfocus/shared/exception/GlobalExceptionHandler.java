package com.devfocus.shared.exception;

import com.devfocus.shared.constants.ErrorCode;
import com.devfocus.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final HttpServletRequest request;

    public GlobalExceptionHandler(HttpServletRequest request) {
        this.request = request;
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handleAppException(AppException appException) {

        return ResponseEntity.status(appException.getHttpStatus()).body(ApiResponse.error(
                appException.getMessage(),
                appException.getErrorCode(),
                request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentException(MethodArgumentNotValidException methodArgumentException) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(
                methodArgumentException.getBindingResult().getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(fieldError -> fieldError.getDefaultMessage())
                        .orElse("Validation failed"),
                ErrorCode.VALIDATION_ERROR,
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception exception) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(
                exception.getMessage(),
                ErrorCode.INTERNAL_ERROR,
                request.getRequestURI()
        ));
    }
}
