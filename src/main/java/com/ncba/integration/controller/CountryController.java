package com.ncba.integration.controller;

import com.ncba.integration.dto.CountryRequest;
import com.ncba.integration.dto.CountryResponse;
import com.ncba.integration.entity.CountryInfo;
import com.ncba.integration.service.CountryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/countries")
@Slf4j
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @PostMapping
    public ResponseEntity<CountryResponse> create(@Valid @RequestBody CountryRequest request) {
        log.info("Creating country: {}", request.countryName());
        CountryInfo country = countryService.createCountry(request.countryName());
        return ResponseEntity.status(HttpStatus.CREATED).body(CountryResponse.from(country));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryResponse> getById(@PathVariable Long id) {
        log.info("Fetching country by id: {}", id);
        CountryInfo country = countryService.getCountryById(id);
        return ResponseEntity.ok(CountryResponse.from(country));
    }

    @GetMapping
    public ResponseEntity<List<CountryResponse>> getAll() {
        log.info("Fetching all countries");
        List<CountryResponse> countries = countryService.getAllCountries()
                .stream()
                .map(CountryResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/iso/{isoCode}")
    public ResponseEntity<CountryResponse> getByIsoCode(@PathVariable String isoCode) {
        log.info("Fetching country by ISO code: {}", isoCode);
        CountryInfo country = countryService.getCountryByIsoCode(isoCode);
        return ResponseEntity.ok(CountryResponse.from(country));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryResponse> update(@PathVariable Long id, @RequestBody CountryInfo updates) {
        log.info("Updating country: id={}", id);
        CountryInfo country = countryService.updateCountry(id, updates);
        return ResponseEntity.ok(CountryResponse.from(country));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting country: id={}", id);
        countryService.deleteCountry(id);
        return ResponseEntity.noContent().build();
    }
}
