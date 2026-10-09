package com.ncba.integration.dto;

import com.ncba.integration.entity.CountryInfo;
import com.ncba.integration.entity.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryResponse {
    private Long id;
    private String name;
    private String isoCode;
    private String capital;
    private String area;
    private String population;
    private String continent;
    private String currencyCode;
    private String currencyName;
    private String phonePrefix;
    private List<String> languages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CountryResponse from(CountryInfo country) {
        return CountryResponse.builder()
                .id(country.getId())
                .name(country.getName())
                .isoCode(country.getIsoCode())
                .capital(country.getCapital())
                .area(country.getArea())
                .population(country.getPopulation())
                .continent(country.getContinent())
                .currencyCode(country.getCurrencyCode())
                .currencyName(country.getCurrencyName())
                .phonePrefix(country.getPhonePrefix())
                .languages(country.getLanguages().stream()
                        .map(Language::getName)
                        .collect(Collectors.toList()))
                .createdAt(country.getCreatedAt())
                .updatedAt(country.getUpdatedAt())
                .build();
    }
}
