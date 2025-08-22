package com.testtask.repository;

import com.testtask.model.CryptoRate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface CryptoRateRepository extends ReactiveCrudRepository<CryptoRate, Long> {
    @Query("""
           select distinct on (name) id, name, value, updated_at
           from crypto_rates
           order by name, updated_at desc
           """)
    Flux<CryptoRate> findLatest();
}
