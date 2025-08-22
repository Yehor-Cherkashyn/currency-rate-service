package com.testtask.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CryptoRateDto(
        @JsonProperty("currency")
        @JsonAlias("name")
        String currency,
        @JsonProperty("rate")
        @JsonAlias("value")
        double rate) {
}
