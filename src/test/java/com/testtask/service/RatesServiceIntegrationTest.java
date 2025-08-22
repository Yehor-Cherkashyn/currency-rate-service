package com.testtask.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import com.testtask.model.CryptoRate;
import com.testtask.model.FiatRate;
import com.testtask.repository.CryptoRateRepository;
import com.testtask.repository.FiatRateRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
class RatesServiceIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("currency_rates")
            .withUsername("currency_user")
            .withPassword("currency_pass_123");

    @BeforeAll
    static void startDb() { POSTGRES.start(); }

    @DynamicPropertySource
    static void r2dbcProps(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" +
                POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/currency_rates");
        registry.add("spring.r2dbc.username", () -> "currency_user");
        registry.add("spring.r2dbc.password", () -> "currency_pass_123");
    }

    @Autowired FiatRateRepository fiatRepo;
    @Autowired CryptoRateRepository cryptoRepo;
    @Autowired RatesService ratesService;

    @MockitoBean
    RatesMockClientService mockClient;

    @Test
    void migrationsApplied_andFallbackWorks() {
        OffsetDateTime ts = OffsetDateTime.now().minusMinutes(5);
        StepVerifier.create(
                fiatRepo.deleteAll()
                        .thenMany(cryptoRepo.deleteAll())
                        .thenMany(fiatRepo.saveAll(Flux.just(
                                FiatRate.builder().currency("EUR").rate(43.2).updatedAt(ts).build(),
                                FiatRate.builder().currency("USD").rate(40.5).updatedAt(ts).build()
                        )))
                        .thenMany(cryptoRepo.saveAll(Flux.just(
                                CryptoRate.builder().name("BTC").value(65000.0).updatedAt(ts).build(),
                                CryptoRate.builder().name("ETH").value(3200.0).updatedAt(ts).build()
                        )))
                        .then()
        ).verifyComplete();

        when(mockClient.fetchFiat()).thenReturn(Flux.empty());
        when(mockClient.fetchCrypto()).thenReturn(Flux.empty());

        StepVerifier.create(ratesService.getCurrencyRates())
                .assertNext(resp -> {
                    assertEquals(2, resp.fiat().size());
                    assertEquals(2, resp.crypto().size());
                    assertTrue(resp.fiat().stream()
                            .anyMatch(f -> f.currency().equals("EUR") && f.rate() == 43.2));
                    assertTrue(resp.crypto().stream()
                            .anyMatch(c -> c.currency().equals("BTC") && c.rate() == 65000.0));
                })
                .verifyComplete();
    }

    @Test
    void liveDataIsAppended_andMergedWithDbSnapshot() {
        OffsetDateTime oldTs = OffsetDateTime.now().minusMinutes(10);
        StepVerifier.create(
                fiatRepo.deleteAll()
                        .thenMany(cryptoRepo.deleteAll())
                        .thenMany(fiatRepo.saveAll(Flux.just(
                                FiatRate.builder().currency("EUR").rate(43.0).updatedAt(oldTs).build(),
                                FiatRate.builder().currency("USD").rate(40.0).updatedAt(oldTs).build()
                        )))
                        .thenMany(cryptoRepo.saveAll(Flux.just(
                                CryptoRate.builder().name("ETH").value(3100.0).updatedAt(oldTs).build()
                        )))
                        .then()
        ).verifyComplete();

        when(mockClient.fetchFiat()).thenReturn(Flux.fromIterable(List.of(
                new com.testtask.dto.FiatRateDto("USD", 41.0)
        )));
        when(mockClient.fetchCrypto()).thenReturn(Flux.fromIterable(List.of(
                new com.testtask.dto.CryptoRateDto("BTC", 66000.0)
        )));

        StepVerifier.create(ratesService.getCurrencyRates())
                .assertNext(resp -> {
                    assertEquals(2, resp.fiat().size());
                    assertEquals(2, resp.crypto().size());
                    assertTrue(resp.fiat().stream()
                            .anyMatch(f -> f.currency().equals("USD") && f.rate() == 41.0));
                    assertTrue(resp.fiat().stream()
                            .anyMatch(f -> f.currency().equals("EUR") && f.rate() == 43.0));
                    assertTrue(resp.crypto().stream()
                            .anyMatch(c -> c.currency().equals("BTC") && c.rate() == 66000.0));
                    assertTrue(resp.crypto().stream()
                            .anyMatch(c -> c.currency().equals("ETH") && c.rate() == 3100.0));
                })
                .verifyComplete();

        StepVerifier.create(fiatRepo.findAll().collectList())
                .assertNext(all -> assertTrue(all.stream()
                        .anyMatch(e -> e.getCurrency().equals("USD") && e.getRate() == 41.0)))
                .verifyComplete();
        StepVerifier.create(cryptoRepo.findAll().collectList())
                .assertNext(all -> assertTrue(all.stream()
                        .anyMatch(e -> e.getName().equals("BTC") && e.getValue() == 66000.0)))
                .verifyComplete();
    }

    @Test
    void persistenceAcrossCalls_liveThenFallbackUsesDb() {
        when(mockClient.fetchFiat()).thenReturn(Flux.fromIterable(List.of(
                new com.testtask.dto.FiatRateDto("USD", 42.0)
        )));
        when(mockClient.fetchCrypto()).thenReturn(Flux.fromIterable(List.of(
                new com.testtask.dto.CryptoRateDto("BTC", 67000.0)
        )));

        StepVerifier.create(ratesService.getCurrencyRates())
                .assertNext(resp -> {
                    assertEquals(1, resp.fiat().size());
                    assertEquals(1, resp.crypto().size());
                })
                .verifyComplete();

        when(mockClient.fetchFiat()).thenReturn(Flux.empty());
        when(mockClient.fetchCrypto()).thenReturn(Flux.empty());

        StepVerifier.create(ratesService.getCurrencyRates())
                .assertNext(resp -> {
                    assertTrue(resp.fiat().stream()
                            .anyMatch(f -> f.currency().equals("USD") && f.rate() == 42.0));
                    assertTrue(resp.crypto().stream()
                            .anyMatch(c -> c.currency().equals("BTC") && c.rate() == 67000.0));
                })
                .verifyComplete();
    }

    @Test
    void emptyLiveAndEmptyDb_returnsEmptyArrays() {
        StepVerifier.create(
                fiatRepo.deleteAll().thenMany(cryptoRepo.deleteAll()).then()
        ).verifyComplete();

        when(mockClient.fetchFiat()).thenReturn(Flux.empty());
        when(mockClient.fetchCrypto()).thenReturn(Flux.empty());

        StepVerifier.create(ratesService.getCurrencyRates())
                .assertNext(resp -> {
                    assertTrue(resp.fiat().isEmpty());
                    assertTrue(resp.crypto().isEmpty());
                })
                .verifyComplete();
    }
}
