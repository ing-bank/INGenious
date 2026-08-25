package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

/**
 * Tests for {@link InlineObjectProperty} — inline object-property override parsing,
 * serialization, token extraction and map mutation.
 */
public class InlineObjectPropertyTest {

    // ── isInline / isGlobal ────────────────────────────────────────────────

    @Test
    public void testIsInlineDetectsObjectMarker() {
        assertThat(InlineObjectProperty.isInline("setProp: #id=Data:Sku")).isTrue();
        assertThat(InlineObjectProperty.isInline("  SETPROP:  #id=1 ")).isTrue();
    }

    @Test
    public void testIsInlineDetectsGlobalMarker() {
        assertThat(InlineObjectProperty.isInline("setGlobalProp: #env=Prod")).isTrue();
        assertThat(InlineObjectProperty.isInline("setglobalprop:#env=Prod")).isTrue();
    }

    @Test
    public void testIsInlineFalseForOrdinaryConditions() {
        assertThat(InlineObjectProperty.isInline("")).isFalse();
        assertThat(InlineObjectProperty.isInline(null)).isFalse();
        assertThat(InlineObjectProperty.isInline("Start Loop")).isFalse();
        assertThat(InlineObjectProperty.isInline("GlobalObject")).isFalse();
    }

    @Test
    public void testIsGlobal() {
        assertThat(InlineObjectProperty.isGlobal("setGlobalProp: #a=1")).isTrue();
        assertThat(InlineObjectProperty.isGlobal("setProp: #a=1")).isFalse();
        assertThat(InlineObjectProperty.isGlobal(null)).isFalse();
    }

    // ── stripMarker ────────────────────────────────────────────────────────

    @Test
    public void testStripMarkerObject() {
        assertThat(InlineObjectProperty.stripMarker("setProp: #id=Data:Sku"))
            .isEqualTo("#id=Data:Sku");
    }

    @Test
    public void testStripMarkerGlobal() {
        assertThat(InlineObjectProperty.stripMarker("setGlobalProp:  #env=%e% "))
            .isEqualTo("#env=%e%");
    }

    // ── parsePairs ─────────────────────────────────────────────────────────

    @Test
    public void testParseSinglePair() {
        List<String[]> pairs = InlineObjectProperty.parsePairs("#id=Products:SKU");
        assertThat(pairs).hasSize(1);
        assertThat(pairs.get(0)).containsExactly("#id", "Products:SKU", "");
    }

    @Test
    public void testParseMultiplePairs() {
        List<String[]> pairs = InlineObjectProperty.parsePairs(
            "#rowId=%currentRow%; #status=Data:State"
        );
        assertThat(pairs).hasSize(2);
        assertThat(pairs.get(0)).containsExactly("#rowId", "%currentRow%", "");
        assertThat(pairs.get(1)).containsExactly("#status", "Data:State", "");
    }

    @Test
    public void testParseSplitsOnFirstEqualsOnly() {
        // value itself contains '=' (e.g. a URL query)
        List<String[]> pairs = InlineObjectProperty.parsePairs("#url=http://host?a=1&b=2");
        assertThat(pairs).hasSize(1);
        assertThat(pairs.get(0)).containsExactly("#url", "http://host?a=1&b=2", "");
    }

    @Test
    public void testParseIgnoresMalformedPairs() {
        List<String[]> pairs = InlineObjectProperty.parsePairs("#a=1; garbage; ; #b=2");
        assertThat(pairs).hasSize(2);
        assertThat(pairs.get(0)).containsExactly("#a", "1", "");
        assertThat(pairs.get(1)).containsExactly("#b", "2", "");
    }

    @Test
    public void testParseSubIteration() {
        List<String[]> pairs = InlineObjectProperty.parsePairs("#id=Products:SKU|subiter=3");
        assertThat(pairs).hasSize(1);
        assertThat(pairs.get(0)).containsExactly("#id", "Products:SKU", "3");
    }

    @Test
    public void testParseSubIterationMixedWithPlainPair() {
        List<String[]> pairs = InlineObjectProperty.parsePairs("#a=Sheet:Col|subiter=2; #b=%v%");
        assertThat(pairs).hasSize(2);
        assertThat(pairs.get(0)).containsExactly("#a", "Sheet:Col", "2");
        assertThat(pairs.get(1)).containsExactly("#b", "%v%", "");
    }

    @Test
    public void testParseEmptyReturnsEmptyList() {
        assertThat(InlineObjectProperty.parsePairs("")).isEmpty();
        assertThat(InlineObjectProperty.parsePairs(null)).isEmpty();
    }

    // ── serialize (round-trip) ─────────────────────────────────────────────

    @Test
    public void testSerializeObjectScope() {
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] { "#id", "Data:Sku" });
        pairs.add(new String[] { "#row", "%r%" });
        String out = InlineObjectProperty.serialize(false, pairs);
        assertThat(out).isEqualTo("setProp: #id=Data:Sku; #row=%r%");
        // round-trips back to the same pairs
        assertThat(InlineObjectProperty.parsePairs(InlineObjectProperty.stripMarker(out)))
            .hasSize(2);
    }

    @Test
    public void testSerializeGlobalScope() {
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] { "#env", "Config:Env" });
        assertThat(InlineObjectProperty.serialize(true, pairs))
            .isEqualTo("setGlobalProp: #env=Config:Env");
    }

    @Test
    public void testSerializeWithSubIterationRoundTrips() {
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] { "#id", "Products:SKU", "3" });
        String out = InlineObjectProperty.serialize(false, pairs);
        assertThat(out).isEqualTo("setProp: #id=Products:SKU|subiter=3");
        assertThat(InlineObjectProperty.parsePairs(InlineObjectProperty.stripMarker(out)).get(0))
            .containsExactly("#id", "Products:SKU", "3");
    }

    @Test
    public void testSerializeEmptySubIterationOmitsMarker() {
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] { "#id", "Data:Sku", "" });
        assertThat(InlineObjectProperty.serialize(false, pairs)).isEqualTo("setProp: #id=Data:Sku");
    }

    // ── extractTokens ──────────────────────────────────────────────────────

    @Test
    public void testExtractTokensFromLocator() {
        assertThat(
                InlineObjectProperty.extractTokens(
                    "//div[@id='#productId']//span[@data-row='#rowId']"
                )
            )
            .containsExactly("#productId", "#rowId");
    }

    @Test
    public void testExtractTokensDeduplicatesPreservingOrder() {
        assertThat(InlineObjectProperty.extractTokens("#a #b #a")).containsExactly("#a", "#b");
    }

    @Test
    public void testExtractTokensNoneReturnsEmpty() {
        assertThat(InlineObjectProperty.extractTokens("//div[@id='static']")).isEmpty();
        assertThat(InlineObjectProperty.extractTokens(null)).isEmpty();
    }

    // ── putObjectProperty ──────────────────────────────────────────────────

    @Test
    public void testPutObjectPropertyCreatesNestedMaps() {
        Map<String, Map<String, Map<String, String>>> map = new HashMap<>();
        InlineObjectProperty.putObjectProperty(map, "Page1", "Btn1", "#id", "42");
        assertThat(map).containsKey("Page1");
        assertThat(map.get("Page1")).containsKey("Btn1");
        assertThat(map.get("Page1").get("Btn1")).containsEntry("#id", "42");
    }

    @Test
    public void testPutObjectPropertyAddsToExistingObject() {
        Map<String, Map<String, Map<String, String>>> map = new HashMap<>();
        InlineObjectProperty.putObjectProperty(map, "P", "O", "#a", "1");
        InlineObjectProperty.putObjectProperty(map, "P", "O", "#b", "2");
        assertThat(map.get("P").get("O")).containsEntry("#a", "1").containsEntry("#b", "2");
    }

    @Test
    public void testPutObjectPropertyOverwritesSameKey() {
        Map<String, Map<String, Map<String, String>>> map = new HashMap<>();
        InlineObjectProperty.putObjectProperty(map, "P", "O", "#a", "1");
        InlineObjectProperty.putObjectProperty(map, "P", "O", "#a", "9");
        assertThat(map.get("P").get("O")).containsEntry("#a", "9");
    }
}
