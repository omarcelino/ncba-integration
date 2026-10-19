package com.ncba.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SoapFullCountryInfoResponse {

    @JsonProperty("sName")
    private String name;

    @JsonProperty("sCapitalCity")
    private String capitalCity;

    @JsonProperty("sCountryIsoCode")
    private String countryIsoCode;

    @JsonProperty("sAreaInSqKmString")
    private String area;

    @JsonProperty("iPopulation")
    private String population;

    @JsonProperty("sContinent")
    private String continent;

    @JsonProperty("sPhoneCode")
    private String phoneCode;

    @JsonProperty("sCurrencyCode")
    private String currencyCode;

    @JsonProperty("sCurrencyName")
    private String currencyName;

    @JsonProperty("Languages")
    private List<Language> languages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Language {
        @JsonProperty("sName")
        private String name;
    }
}
