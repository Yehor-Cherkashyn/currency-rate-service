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
@Table("crypto_rates")
public class CryptoRate {
    @Id
    private Long id;
    private String name;
    private double value;
    private OffsetDateTime updatedAt;
}
