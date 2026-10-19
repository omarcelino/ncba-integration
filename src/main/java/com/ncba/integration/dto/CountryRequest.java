package com.ncba.integration.dto;

import jakarta.validation.constraints.NotBlank;

public record CountryRequest(
        @NotBlank(message = "Country name is required")
        String countryName ) {
}
