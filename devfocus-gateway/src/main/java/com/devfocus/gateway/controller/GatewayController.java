package com.devfocus.gateway.controller;

import com.devfocus.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> checkHealth(HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Gateway is running", httpServletRequest.getRequestURI()));
    }
}
