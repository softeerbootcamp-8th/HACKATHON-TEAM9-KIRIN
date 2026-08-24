package com.kirin.superservice.health.dto.response;

public record HealthResponse(String status) {

    public static HealthResponse ok() {
        return new HealthResponse("OK");
    }
}
