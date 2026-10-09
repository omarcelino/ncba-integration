package com.ncba.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncba.integration.dto.SoapFullCountryInfoResponse;
import com.ncba.integration.entity.CountryInfo;
import com.ncba.integration.entity.Language;
import com.ncba.integration.repository.CountryInfoRepository;
import com.ncba.integration.soap.CountryInfoSoapClient;
import com.ncba.integration.util.NameNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CountryService {

    private final CountryInfoRepository repository;
    private final CountryInfoSoapClient soapClient;
    private final NameNormalizer nameNormalizer;
    private final ObjectMapper objectMapper;

    public CountryService(CountryInfoRepository repository,
                         CountryInfoSoapClient soapClient,
                         NameNormalizer nameNormalizer,
                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.soapClient = soapClient;
        this.nameNormalizer = nameNormalizer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @CacheEvict(value = "isoCodes", allEntries = true)
    public CountryInfo createCountry(String countryName) {
        String normalized = nameNormalizer.toSentenceCase(countryName);
        log.info("Creating country: normalized='{}'", normalized);

        var existing = repository.findByNameIgnoreCase(normalized);
        if (existing.isPresent()) {
            log.info("Country already exists: {}", normalized);
            return existing.get();
        }

        String isoCode = soapClient.getCountryIsoCode(normalized);
        log.info("Fetched ISO code: {} for country: {}", isoCode, normalized);

        String fullInfoXml = soapClient.getFullCountryInfo(isoCode);
        CountryInfo countryInfo = parseAndSaveCountryInfo(fullInfoXml, normalized);

        log.info("Country created successfully: id={} name={}", countryInfo.getId(), countryInfo.getName());
        return countryInfo;
    }

    @Transactional(readOnly = true)
    @Cacheable("isoCodes")
    public CountryInfo getCountryById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Country not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public CountryInfo getCountryByIsoCode(String isoCode) {
        return repository.findByIsoCode(isoCode)
                .orElseThrow(() -> new RuntimeException("Country not found with ISO code: " + isoCode));
    }

    @Transactional(readOnly = true)
    public List<CountryInfo> getAllCountries() {
        return repository.findAll();
    }

    @Transactional
    @CacheEvict(value = "isoCodes", allEntries = true)
    public CountryInfo updateCountry(Long id, CountryInfo updates) {
        CountryInfo existing = getCountryById(id);

        if (updates.getCapital() != null) existing.setCapital(updates.getCapital());
        if (updates.getArea() != null) existing.setArea(updates.getArea());
        if (updates.getPopulation() != null) existing.setPopulation(updates.getPopulation());
        if (updates.getContinent() != null) existing.setContinent(updates.getContinent());
        if (updates.getCurrencyCode() != null) existing.setCurrencyCode(updates.getCurrencyCode());
        if (updates.getCurrencyName() != null) existing.setCurrencyName(updates.getCurrencyName());
        if (updates.getPhonePrefix() != null) existing.setPhonePrefix(updates.getPhonePrefix());

        log.info("Country updated: id={} name={}", existing.getId(), existing.getName());
        return repository.save(existing);
    }

    @Transactional
    @CacheEvict(value = "isoCodes", allEntries = true)
    public void deleteCountry(Long id) {
        repository.deleteById(id);
        log.info("Country deleted: id={}", id);
    }

    private CountryInfo parseAndSaveCountryInfo(String fullInfoXml, String countryName) throws RuntimeException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(fullInfoXml)));
            XPath xpath = XPathFactory.newInstance().newXPath();

            CountryInfo country = CountryInfo.builder()
                    .name(countryName)
                    .isoCode(extractXPathValue(xpath, doc, "//sCountryISOCode/text()"))
                    .capital(extractXPathValue(xpath, doc, "//sCapitalCity/text()"))
                    .area(extractXPathValue(xpath, doc, "//sAreaInSqKmString/text()"))
                    .population(extractXPathValue(xpath, doc, "//iPopulation/text()"))
                    .continent(extractXPathValue(xpath, doc, "//sContinent/text()"))
                    .currencyCode(extractXPathValue(xpath, doc, "//sCurrencyCode/text()"))
                    .currencyName(extractXPathValue(xpath, doc, "//sCurrencyName/text()"))
                    .phonePrefix(extractXPathValue(xpath, doc, "//sPhoneCode/text()"))
                    .build();

            NodeList languageNodes = (NodeList) xpath.evaluate("//Languages/Language/sName/text()", doc, XPathConstants.NODESET);
            for (int i = 0; i < languageNodes.getLength(); i++) {
                String languageName = languageNodes.item(i).getNodeValue();
                country.getLanguages().add(Language.builder()
                        .name(languageName)
                        .country(country)
                        .build());
            }

            return repository.save(country);
        } catch (Exception e) {
            log.error("Failed to parse country info XML", e);
            throw new RuntimeException("Failed to parse country info: " + e.getMessage(), e);
        }
    }

    private String extractXPathValue(XPath xpath, Document doc, String expression) {
        try {
            Node node = (Node) xpath.evaluate(expression, doc, XPathConstants.NODE);
            return node != null ? node.getNodeValue() : null;
        } catch (Exception e) {
            log.debug("Could not extract value for expression: {}", expression, e);
            return null;
        }
    }
}
