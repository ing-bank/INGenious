package com.ing.engine.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** Conformance tests for the ConventionCatalog input grammar helpers. */
public class ConventionCatalogTest {

    @Test
    public void dataRefGrammar() {
        assertThat(ConventionCatalog.isDataRef("LoginData:Username")).isTrue();
        assertThat(ConventionCatalog.isDataRef("Sheet-1:Col.2")).isTrue();
        // Not data refs:
        assertThat(ConventionCatalog.isDataRef("@literal")).isFalse();
        assertThat(ConventionCatalog.isDataRef("https://example.com")).isFalse();
        assertThat(ConventionCatalog.isDataRef("Content-Type: application/json")).isFalse();
        assertThat(ConventionCatalog.isDataRef("#test")).isFalse();
        assertThat(ConventionCatalog.isDataRef("")).isFalse();
        assertThat(ConventionCatalog.isDataRef(null)).isFalse();
    }

    @Test
    public void payloadTokenGrammar() {
        assertThat(ConventionCatalog.containsPayloadTokens("{Payment:AccountNumber}")).isTrue();
        assertThat(ConventionCatalog.containsPayloadTokens("{\"a\":\"{Sheet:Col}\"}")).isTrue();
        assertThat(ConventionCatalog.containsPayloadTokens("{\"a\":\"plain\"}")).isFalse();
        assertThat(ConventionCatalog.containsPayloadTokens(null)).isFalse();
    }

    @Test
    public void literalClassification() {
        assertThat(ConventionCatalog.isParameterizableLiteral("@200")).isTrue();
        assertThat(ConventionCatalog.isParameterizableLiteral("@https://x")).isTrue();
        // Engine directives and grammar-conformant inputs are not candidates:
        assertThat(ConventionCatalog.isParameterizableLiteral("@Browser")).isFalse();
        assertThat(ConventionCatalog.isParameterizableLiteral("@Enter")).isFalse();
        assertThat(ConventionCatalog.isParameterizableLiteral("Sheet:Col")).isFalse();
        assertThat(ConventionCatalog.isParameterizableLiteral("@")).isFalse();
        assertThat(ConventionCatalog.isParameterizableLiteral("")).isFalse();
        // @Sheet:Col is a literal that *looks* like a data ref – leave it alone.
        assertThat(ConventionCatalog.isParameterizableLiteral("@Sheet:Col")).isFalse();
    }

    @Test
    public void payloadActions() {
        assertThat(ConventionCatalog.isPayloadAction("postRestRequest")).isTrue();
        assertThat(ConventionCatalog.isPayloadAction("PutRestRequest")).isTrue();
        assertThat(ConventionCatalog.isPayloadAction("patchRestRequest")).isTrue();
        assertThat(ConventionCatalog.isPayloadAction("deleteWithPayload")).isTrue();
        assertThat(ConventionCatalog.isPayloadAction("getRestRequest")).isFalse();
        assertThat(ConventionCatalog.isPayloadAction("setEndPoint")).isFalse();
        assertThat(ConventionCatalog.isPayloadAction(null)).isFalse();
    }

    @Test
    public void globalDataIds() {
        assertThat(ConventionCatalog.isGlobalDataId("#test")).isTrue();
        assertThat(ConventionCatalog.isGlobalDataId("#")).isFalse();
        assertThat(ConventionCatalog.isGlobalDataId("test")).isFalse();
    }

    @Test
    public void renderedDocsMentionEveryRule() {
        String doc = ConventionCatalog.conventionsDoc();
        for (ConventionCatalog.Rule r : ConventionCatalog.all()) {
            assertThat(doc).as("conventionsDoc mentions rule %s", r.id).contains(r.id);
        }
        // The condensed handshake text covers the core grammar.
        String condensed = ConventionCatalog.condensedInstructions();
        assertThat(condensed).contains("Sheet:Column");
        assertThat(condensed).contains("{Sheet:Column}");
        assertThat(condensed).contains("@-prefixed");
        assertThat(condensed).contains("Execute");
        assertThat(condensed).contains("ingenious_testcase_parameterize");
    }
}
