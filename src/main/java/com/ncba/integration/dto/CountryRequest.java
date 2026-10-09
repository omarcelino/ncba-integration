package com.ncba.integration.dto;

public record CountryRequest(
        @NotBlank(message = "Country name is required")
        //and size if required
        String countryName ) {
}
