package com.testtask.util;

import java.time.OffsetDateTime;
import com.testtask.dto.CryptoRateDto;
import com.testtask.dto.FiatRateDto;
import com.testtask.model.CryptoRate;
import com.testtask.model.FiatRate;

public class DtoMapper {
    private DtoMapper() {}

    public static FiatRate toEntity(FiatRateDto dto, OffsetDateTime dateTime) {
        return FiatRate.builder()
                .currency(dto.currency())
                .rate(dto.rate())
                .updatedAt(dateTime)
                .build();
    }

    public static CryptoRate toEntity(CryptoRateDto dto, OffsetDateTime dateTime) {
        return CryptoRate.builder()
                .name(dto.currency())
                .value(dto.rate())
                .updatedAt(dateTime)
                .build();
    }

    public static FiatRateDto toDto(FiatRate entity) {
        return new FiatRateDto(entity.getCurrency(), entity.getRate());
    }

    public static CryptoRateDto toDto(CryptoRate entity) {
        return new CryptoRateDto(entity.getName(), entity.getValue());
    }
}
