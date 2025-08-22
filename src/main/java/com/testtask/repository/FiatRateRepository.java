package com.testtask.repository;

import com.testtask.model.FiatRate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface FiatRateRepository extends ReactiveCrudRepository<FiatRate, Long> {
    @Query("""
           select distinct on (currency) id, currency, rate, updated_at
           from fiat_rates
           order by currency, updated_at desc
           """)
    Flux<FiatRate> findLatest();
}
