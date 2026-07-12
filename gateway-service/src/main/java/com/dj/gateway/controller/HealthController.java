package com.dj.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Health check endpoint")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check", description = "Returns service health status.")
    @ApiResponse(responseCode = "200", description = "Service is healthy", content = @Content(schema = @Schema(example = "{\"status\":\"UP\"}")))
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
