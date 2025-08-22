package com.testtask.controller;

import java.util.List;
import com.testtask.dto.CurrencyRatesResponseDto;
import com.testtask.service.RatesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CurrencyRatesController {
    private final RatesService ratesService;

    @GetMapping(path = "/currency-rates", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CurrencyRatesResponseDto> getRates() {
        final long started = System.nanoTime();
        return ratesService.getCurrencyRates()
                .onErrorResume(e -> {
                    log.error("GET /currency-rates -> handled error, returning empty snapshot: {}", e.toString());
                    return Mono.just(new CurrencyRatesResponseDto(List.of(), List.of()));
                })
                .doOnSuccess(response -> {
                    long tookMs = (System.nanoTime() - started) / 1_000_000;
                    int fiat = response.fiat() != null ? response.fiat().size() : 0;
                    int crypto = response.crypto() != null ? response.crypto().size() : 0;
                    log.info("GET /currency-rates -> 200, fiat={}, crypto={}, tookMs={}", fiat, crypto, tookMs);
                });
    }
}
