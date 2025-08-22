package com.testtask.service;

import java.time.Duration;
import com.testtask.config.AppProperties;
import com.testtask.dto.CryptoRateDto;
import com.testtask.dto.FiatRateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Slf4j
@Component
public class RatesMockClientService {
    private final WebClient webClient;
    private final AppProperties.Mocks mocks;
    private final AppProperties.Http http;

    public RatesMockClientService(WebClient.Builder webClientBuilder, AppProperties properties) {
        this.mocks = properties.getMocks();
        this.http = properties.getHttp();

        this.webClient = webClientBuilder
                .baseUrl(mocks.getBaseUrl())
                .defaultHeader(mocks.getApiKeyHeader(), mocks.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Flux<FiatRateDto> fetchFiat() {
        return webClient.get()
                .uri(mocks.getFiatPath())
                .retrieve()
                .bodyToFlux(FiatRateDto.class)
                .timeout(Duration.ofSeconds(http.getTimeoutSeconds()))
                .retryWhen(Retry.backoff(http.getRetryMaxAttempts(), Duration.ofMillis(http.getRetryBackoffMs())))
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException we) {
                        log.warn("Fiat fetch failed: status={} body={}",
                                we.getStatusCode(), we.getResponseBodyAsString());
                    } else {
                        log.warn("Fiat fetch failed: {}", e.toString());
                    }
                })
                .onErrorResume(e -> Flux.empty());
    }

    public Flux<CryptoRateDto> fetchCrypto() {
        return webClient.get()
                .uri(mocks.getCryptoPath())
                .retrieve()
                .bodyToFlux(CryptoRateDto.class)
                .timeout(Duration.ofSeconds(http.getTimeoutSeconds()))
                .retryWhen(Retry.backoff(http.getRetryMaxAttempts(), Duration.ofMillis(http.getRetryBackoffMs())))
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException we) {
                        log.warn("Crypto fetch failed: status={} body={}", we.getStatusCode(), we.getResponseBodyAsString());
                    } else {
                        log.warn("Crypto fetch failed: {}", e.toString());
                    }
                })
                .onErrorResume(e -> Flux.empty());
    }
}
