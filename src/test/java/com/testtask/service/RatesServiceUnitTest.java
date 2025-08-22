package com.testtask.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import com.testtask.dto.CryptoRateDto;
import com.testtask.dto.FiatRateDto;
import com.testtask.model.CryptoRate;
import com.testtask.model.FiatRate;
import com.testtask.repository.CryptoRateRepository;
import com.testtask.repository.FiatRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class RatesServiceUnitTest {

    private RatesMockClientService mockClient;
    private FiatRateRepository fiatRepo;
    private CryptoRateRepository cryptoRepo;
    private RatesService service;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(RatesMockClientService.class);
        fiatRepo = Mockito.mock(FiatRateRepository.class);
        cryptoRepo = Mockito.mock(CryptoRateRepository.class);
        service = new RatesService(mockClient, fiatRepo, cryptoRepo);
    }

    @Test
    void liveDataPresent_isAppended_andMergedWithDbSnapshot() {
        List<FiatRateDto> liveFiat = List.of(new FiatRateDto("USD", 40.5));
        List<CryptoRateDto> liveCrypto = List.of(new CryptoRateDto("BTC", 65000.0));

        when(mockClient.fetchFiat()).thenReturn(Flux.fromIterable(liveFiat));
        when(mockClient.fetchCrypto()).thenReturn(Flux.fromIterable(liveCrypto));

        when(fiatRepo.saveAll(any(Flux.class))).thenReturn(Flux.empty());
        when(cryptoRepo.saveAll(any(Flux.class))).thenReturn(Flux.empty());

        OffsetDateTime ts = OffsetDateTime.now().minusMinutes(5);
        when(fiatRepo.findLatest()).thenReturn(Flux.just(
                FiatRate.builder().id(1L).currency("EUR").rate(43.2).updatedAt(ts).build()
        ));
        when(cryptoRepo.findLatest()).thenReturn(Flux.just(
                CryptoRate.builder().id(1L).name("ETH").value(3200.0).updatedAt(ts).build()
        ));

        StepVerifier.create(service.getCurrencyRates())
                .assertNext(resp -> {
                    // очікуємо USD (LIVE) + EUR (DB)
                    assertEquals(2, resp.fiat().size());
                    assertTrue(resp.fiat().stream().anyMatch(f -> f.currency().equals("USD") && f.rate() == 40.5));
                    assertTrue(resp.fiat().stream().anyMatch(f -> f.currency().equals("EUR") && f.rate() == 43.2));
                    // та BTC (LIVE) + ETH (DB)
                    assertEquals(2, resp.crypto().size());
                    assertTrue(resp.crypto().stream().anyMatch(c -> c.currency().equals("BTC") && c.rate() == 65000.0));
                    assertTrue(resp.crypto().stream().anyMatch(c -> c.currency().equals("ETH") && c.rate() == 3200.0));
                })
                .verifyComplete();
    }

    @Test
    void liveDataEmpty_fallsBackToDbLatest() {
        when(mockClient.fetchFiat()).thenReturn(Flux.empty());
        when(mockClient.fetchCrypto()).thenReturn(Flux.empty());

        OffsetDateTime ts = OffsetDateTime.now().minusMinutes(10);
        when(fiatRepo.findLatest()).thenReturn(Flux.just(
                FiatRate.builder().id(1L).currency("EUR").rate(43.1).updatedAt(ts).build(),
                FiatRate.builder().id(2L).currency("USD").rate(40.4).updatedAt(ts).build()
        ));
        when(cryptoRepo.findLatest()).thenReturn(Flux.just(
                CryptoRate.builder().id(1L).name("BTC").value(64000.0).updatedAt(ts).build(),
                CryptoRate.builder().id(2L).name("ETH").value(3100.0).updatedAt(ts).build()
        ));

        StepVerifier.create(service.getCurrencyRates())
                .assertNext(resp -> {
                    assertEquals(2, resp.fiat().size());
                    assertEquals(2, resp.crypto().size());
                    assertTrue(resp.fiat().stream().anyMatch(f -> f.currency().equals("EUR")));
                    assertTrue(resp.crypto().stream().anyMatch(c -> c.currency().equals("BTC")));
                })
                .verifyComplete();
    }

    @Test
    void allEmpty_liveAndDb_returnsEmptyArrays() {
        when(mockClient.fetchFiat()).thenReturn(Flux.empty());
        when(mockClient.fetchCrypto()).thenReturn(Flux.empty());
        when(fiatRepo.findLatest()).thenReturn(Flux.empty());
        when(cryptoRepo.findLatest()).thenReturn(Flux.empty());

        StepVerifier.create(service.getCurrencyRates())
                .assertNext(resp -> {
                    assertTrue(resp.fiat().isEmpty());
                    assertTrue(resp.crypto().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void dbReadErrors_areSwallowed_andReturnEmpty() {
        when(mockClient.fetchFiat()).thenReturn(Flux.empty());
        when(mockClient.fetchCrypto()).thenReturn(Flux.empty());
        when(fiatRepo.findLatest()).thenReturn(Flux.error(new RuntimeException("db down")));
        when(cryptoRepo.findLatest()).thenReturn(Flux.error(new RuntimeException("db down")));

        StepVerifier.create(service.getCurrencyRates())
                .assertNext(resp -> {
                    assertTrue(resp.fiat().isEmpty());
                    assertTrue(resp.crypto().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void saveErrors_doNotBreakResponse_liveWins() {
        List<FiatRateDto> liveFiat = List.of(new FiatRateDto("USD", 41.0));
        List<CryptoRateDto> liveCrypto = List.of(new CryptoRateDto("BTC", 66000.0));

        when(mockClient.fetchFiat()).thenReturn(Flux.fromIterable(liveFiat));
        when(mockClient.fetchCrypto()).thenReturn(Flux.fromIterable(liveCrypto));

        when(fiatRepo.saveAll(any(Flux.class))).thenReturn(Flux.error(new RuntimeException("write failed")));
        when(cryptoRepo.saveAll(any(Flux.class))).thenReturn(Flux.error(new RuntimeException("write failed")));

        when(fiatRepo.findLatest()).thenReturn(Flux.empty());
        when(cryptoRepo.findLatest()).thenReturn(Flux.empty());

        StepVerifier.create(service.getCurrencyRates())
                .assertNext(resp -> {
                    assertEquals(1, resp.fiat().size());
                    assertEquals("USD", resp.fiat().getFirst().currency());
                    assertEquals(41.0, resp.fiat().getFirst().rate(), 1e-9);

                    assertEquals(1, resp.crypto().size());
                    assertEquals("BTC", resp.crypto().getFirst().currency());
                    assertEquals(66000.0, resp.crypto().getFirst().rate(), 1e-9);
                })
                .verifyComplete();
    }

    @Test
    void partialLive_isMergedWithDb_latestWinsPerKey() {
        when(mockClient.fetchFiat()).thenReturn(Flux.just(new FiatRateDto("USD", 40.5)));
        when(mockClient.fetchCrypto()).thenReturn(Flux.just(new CryptoRateDto("BTC", 65000.0)));

        OffsetDateTime ts = OffsetDateTime.now().minusMinutes(5);
        when(fiatRepo.saveAll(any(Flux.class))).thenReturn(Flux.empty());
        when(cryptoRepo.saveAll(any(Flux.class))).thenReturn(Flux.empty());
        when(fiatRepo.findLatest()).thenReturn(Flux.just(
                FiatRate.builder().currency("EUR").rate(43.2).updatedAt(ts).build()
        ));
        when(cryptoRepo.findLatest()).thenReturn(Flux.just(
                CryptoRate.builder().name("ETH").value(3200.0).updatedAt(ts).build()
        ));

        StepVerifier.create(service.getCurrencyRates())
                .assertNext(resp -> {
                    assertEquals(2, resp.fiat().size());
                    assertEquals(2, resp.crypto().size());
                })
                .verifyComplete();
    }
}
