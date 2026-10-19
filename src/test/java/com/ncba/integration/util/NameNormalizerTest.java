package com.ncba.integration.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NameNormalizerTest {

    private final NameNormalizer normalizer = new NameNormalizer();

    @Test void lowercaseBecomesSentenceCase() { assertEquals("Kenya", normalizer.toSentenceCase("kenya")); }
    @Test void uppercaseBecomesSentenceCase() { assertEquals("Tanzania", normalizer.toSentenceCase("TANZANIA")); }
    @Test void trimsAndCollapsesSpaces() { assertEquals("Uganda", normalizer.toSentenceCase("  uganda  ")); }
    @Test void blankIsRejected() { assertThrows(IllegalArgumentException.class, () -> normalizer.toSentenceCase("   ")); }
}