package com.ncba.integration.soap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
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

@Component
@Slf4j
public class CountryInfoSoapClient {

    private final RestTemplate restTemplate;
    private final String soapUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public CountryInfoSoapClient(RestTemplate restTemplate,
                                  @Value("${soap.url}") String soapUrl,
                                  @Value("${soap.connect-timeout-ms}") int connectTimeoutMs,
                                  @Value("${soap.read-timeout-ms}") int readTimeoutMs) {
        this.restTemplate = restTemplate;
        this.soapUrl = soapUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getCountryIsoCode(String countryName) {
        String soapRequest = buildCountryIsoCodeRequest(countryName);
        try {
            String response = callSoapService(soapRequest);
            return extractCountryIsoCode(response);
        } catch (Exception e) {
            log.error("Failed to fetch ISO code for country: {}", countryName, e);
            throw new RuntimeException("Failed to fetch ISO code for country: " + countryName, e);
        }
    }

    public String getFullCountryInfo(String isoCode) {
        String soapRequest = buildFullCountryInfoRequest(isoCode);
        try {
            return callSoapService(soapRequest);
        } catch (Exception e) {
            log.error("Failed to fetch full country info for ISO code: {}", isoCode, e);
            throw new RuntimeException("Failed to fetch country info for ISO code: " + isoCode, e);
        }
    }

    private String callSoapService(String soapRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", "");

        HttpEntity<String> entity = new HttpEntity<>(soapRequest, headers);

        try {
            return restTemplate.postForObject(soapUrl, entity, String.class);
        } catch (RestClientException e) {
            log.error("SOAP service call failed", e);
            throw new RuntimeException("SOAP service call failed: " + e.getMessage(), e);
        }
    }

    private String buildCountryIsoCodeRequest(String countryName) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "<CountryISOCode xmlns=\"http://www.oorsprong.org/websamples.countryinfo\">" +
                "<sCountryName>" + escapeXml(countryName) + "</sCountryName>" +
                "</CountryISOCode>" +
                "</soap:Body>" +
                "</soap:Envelope>";
    }

    private String buildFullCountryInfoRequest(String isoCode) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "<FullCountryInfo xmlns=\"http://www.oorsprong.org/websamples.countryinfo\">" +
                "<sCountryISOCode>" + escapeXml(isoCode) + "</sCountryISOCode>" +
                "</FullCountryInfo>" +
                "</soap:Body>" +
                "</soap:Envelope>";
    }

    private String extractCountryIsoCode(String soapResponse) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(soapResponse)));

        XPath xpath = XPathFactory.newInstance().newXPath();
        Node node = (Node) xpath.evaluate("//CountryISOCodeResult/text()", doc, XPathConstants.NODE);

        if (node != null && !node.getNodeValue().isEmpty()) {
            String isoCode = node.getNodeValue();
            log.info("Extracted ISO code: {}", isoCode);
            return isoCode;
        }
        throw new RuntimeException("Could not extract ISO code from SOAP response");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
