package com.ncba.integration.controller;

import com.ncba.integration.dto.CountryRequest;
import com.ncba.integration.util.NameNormalizer;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class CountryController {

    private final NameNormalizer nameNormalizer;

    @PostMapping("/api/countries")
    public ResponseEntity<Map<String, String>> create(@Valid @ResponseBody CountryRequest request) {
        String normalized = nameNormalizer.toSentenceCase(request.countryName());
        log.info("action=receive_country raw='{}' normalized='{}'", request.countryName(), normalized);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("countryName", normalized));
    }
}
