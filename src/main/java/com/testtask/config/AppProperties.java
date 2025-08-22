package com.testtask.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
@Getter
public class AppProperties {
    private final Mocks mocks = new Mocks();
    private final Http http = new Http();

    @Getter
    @Setter
    public static class Mocks {
        @NotBlank
        private String baseUrl;
        @NotBlank
        private String fiatPath;
        @NotBlank
        private String cryptoPath;
        @NotBlank
        private String apiKey;
        @NotBlank
        private String apiKeyHeader = "X-API-KEY";

    }

    @Getter
    @Setter
    public static class Http {
        @Positive
        private int timeoutSeconds = 3;
        @Positive
        private int retryMaxAttempts = 1;
        @Positive
        private long retryBackoffMs = 200;

    }
}
