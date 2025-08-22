package com.testtask.service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.testtask.dto.CryptoRateDto;
import com.testtask.dto.CurrencyRatesResponseDto;
import com.testtask.dto.FiatRateDto;
import com.testtask.model.CryptoRate;
import com.testtask.model.FiatRate;
import com.testtask.repository.CryptoRateRepository;
import com.testtask.repository.FiatRateRepository;
import com.testtask.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatesService {
    private final RatesMockClientService mockClient;
    private final FiatRateRepository fiatRepo;
    private final CryptoRateRepository cryptoRepo;

    public Mono<CurrencyRatesResponseDto> getCurrencyRates() {
        final OffsetDateTime dateTimeNow = OffsetDateTime.now();

        Mono<List<FiatRateDto>> fiat = mockClient.fetchFiat()
                .collectList()
                .flatMap(live -> {
                    if (live.isEmpty()) {
                        log.info("Fiat: live=0 -> using latest from DB");
                        return latestFiatSnapshot()
                                .doOnNext(db -> log.info("Fiat: db-latest={}", db.size()));
                    } else {
                        return saveFiat(live, dateTimeNow)
                                .then(latestFiatSnapshot())
                                .map(db -> mergeFiat(live, db))
                                .doOnNext(merged -> log.info("Fiat: live={} db={} merged={}",
                                        live.size(), merged.size() - live.size(), merged.size()));
                    }
                });

        Mono<List<CryptoRateDto>> crypto = mockClient.fetchCrypto()
                .collectList()
                .flatMap(live -> {
                    if (live.isEmpty()) {
                        log.info("Crypto: live=0 -> using latest from DB");
                        return latestCryptoSnapshot()
                                .doOnNext(db -> log.info("Crypto: db-latest={}", db.size()));
                    } else {
                        return saveCrypto(live, dateTimeNow)
                                .then(latestCryptoSnapshot())
                                .map(db -> mergeCrypto(live, db))
                                .doOnNext(merged -> log.info("Crypto: live={} db={} merged={}",
                                        live.size(), merged.size() - live.size(), merged.size()));
                    }
                });

        return Mono.zip(fiat, crypto)
                .map(tuple -> new CurrencyRatesResponseDto(tuple.getT1(), tuple.getT2()));
    }

    private Mono<Void> saveFiat(List<FiatRateDto> dtoList, OffsetDateTime dateTime) {
        Flux<FiatRate> stream = Flux.fromIterable(dtoList).map(dto -> DtoMapper.toEntity(dto, dateTime));
        return fiatRepo.saveAll(stream)
                .doOnError(e -> log.warn("Fiat save failed: {}", e.toString()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<Void> saveCrypto(List<CryptoRateDto> listDto, OffsetDateTime dateTime) {
        Flux<CryptoRate> stream = Flux.fromIterable(listDto).map(dto -> DtoMapper.toEntity(dto, dateTime));
        return cryptoRepo.saveAll(stream)
                .doOnError(e -> log.warn("Crypto save failed: {}", e.toString()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<List<FiatRateDto>> latestFiatSnapshot() {
        return fiatRepo.findLatest()
                .map(DtoMapper::toDto)
                .collectList()
                .doOnError(e -> log.warn("Fiat latest DB read failed: {}", e.toString()))
                .onErrorReturn(List.of());
    }

    private Mono<List<CryptoRateDto>> latestCryptoSnapshot() {
        return cryptoRepo.findLatest()
                .map(DtoMapper::toDto)
                .collectList()
                .doOnError(e -> log.warn("Crypto latest DB read failed: {}", e.toString()))
                .onErrorReturn(List.of());
    }

    private static List<FiatRateDto> mergeFiat(List<FiatRateDto> mockDtoList, List<FiatRateDto> dbDtoList) {
        Map<String, FiatRateDto> result = new LinkedHashMap<>();
        for (FiatRateDto dbDto : dbDtoList) result.put(dbDto.currency(), dbDto);
        for (FiatRateDto mockDto : mockDtoList) result.put(mockDto.currency(), mockDto);
        return result.values().stream()
                .sorted(Comparator.comparing(FiatRateDto::currency))
                .collect(Collectors.toList());
    }

    private static List<CryptoRateDto> mergeCrypto(List<CryptoRateDto> mockDtoList, List<CryptoRateDto> dbDtoList) {
        Map<String, CryptoRateDto> result = new LinkedHashMap<>();
        for (CryptoRateDto dbDto : dbDtoList) result.put(dbDto.currency(), dbDto);
        for (CryptoRateDto mockDto : mockDtoList) result.put(mockDto.currency(), mockDto);
        return result.values().stream()
                .sorted(Comparator.comparing(CryptoRateDto::currency))
                .collect(Collectors.toList());
    }
}
