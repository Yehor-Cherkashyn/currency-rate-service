package com.testtask.dto;

import java.util.List;

public record CurrencyRatesResponseDto(
        List<FiatRateDto> fiat,
        List<CryptoRateDto> crypto
) {
}
