package com.testtask.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("fiat_rates")
public class FiatRate {
    @Id
    private Long id;
    private String currency;
    private double rate;
    private OffsetDateTime updatedAt;
}
