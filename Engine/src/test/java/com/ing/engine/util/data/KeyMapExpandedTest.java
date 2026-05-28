package com.ing.engine.util.data;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;

/**
 * Extended KeyMap tests covering edge cases and additional patterns.
 * Complements the existing KeyMapTest.
 */
public class KeyMapExpandedTest {

    // ---- Pattern constants ----

    @Test
    public void testContextVarsPatternMatchesCurlyBraces() {
        java.util.regex.Matcher m = KeyMap.CONTEXT_VARS.matcher("{hello}");
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo("hello");
    }

    @Test
    public void testEnvVarsPatternMatchesDollarCurly() {
        java.util.regex.Matcher m = KeyMap.ENV_VARS.matcher("${MY_VAR}");
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo("MY_VAR");
    }

    @Test
    public void testUserVarsPatternMatchesPercent() {
        java.util.regex.Matcher m = KeyMap.USER_VARS.matcher("%user%");
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo("user");
    }

    // ---- replaceKeys edge cases ----

    @Test
    public void testReplaceKeysEmptyString() {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        String result = KeyMap.replaceKeys("", KeyMap.CONTEXT_VARS, true, 1, map);
        assertThat(result).isEmpty();
    }

    @Test
    public void testReplaceKeysNullMaps() {
        String result = KeyMap.replaceKeys("{key}", KeyMap.CONTEXT_VARS, true, 1, (Map<?, ?>[]) null);
        assertThat(result).isEqualTo("{key}");
    }

    @Test
    public void testReplaceKeysNullMapsStripKeys() {
        String result = KeyMap.replaceKeys("{key}", KeyMap.CONTEXT_VARS, false, 1, (Map<?, ?>[]) null);
        assertThat(result).isEqualTo("key");
    }

    @Test
    public void testReplaceKeysPreserveKeysTrue() {
        Map<String, String> map = new HashMap<>();
        String result = KeyMap.replaceKeys("{missing}", KeyMap.CONTEXT_VARS, true, 1, map);
        assertThat(result).isEqualTo("{missing}");
    }

    @Test
    public void testReplaceKeysPreserveKeysFalse() {
        Map<String, String> map = new HashMap<>();
        String result = KeyMap.replaceKeys("{missing}", KeyMap.CONTEXT_VARS, false, 1, map);
        assertThat(result).isEqualTo("missing");
    }

    @Test
    public void testReplaceKeysMultipleSameVar() {
        Map<String, String> map = new HashMap<>();
        map.put("x", "1");
        String result = KeyMap.replaceKeys("{x}+{x}={x}{x}", KeyMap.CONTEXT_VARS, true, 1, map);
        assertThat(result).isEqualTo("1+1=11");
    }

    @Test
    public void testReplaceKeysMultiplePasses() {
        Map<String, String> map = new HashMap<>();
        map.put("inner", "resolved");
        map.put("outer", "{inner}");
        String result = KeyMap.replaceKeys("{outer}", KeyMap.CONTEXT_VARS, true, 2, map);
        assertThat(result).isEqualTo("resolved");
    }

    @Test
    public void testReplaceKeysMultipleMapsFirstWins() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("k", "first");
        Map<String, String> map2 = new HashMap<>();
        map2.put("k", "second");
        String result = KeyMap.replaceKeys("{k}", KeyMap.CONTEXT_VARS, true, 1, map1, map2);
        assertThat(result).isEqualTo("first");
    }

    @Test
    public void testReplaceKeysAcrossMaps() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "A");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "B");
        String result = KeyMap.replaceKeys("{a}-{b}", KeyMap.CONTEXT_VARS, true, 1, map1, map2);
        assertThat(result).isEqualTo("A-B");
    }

    // ---- resolveContextVars ----

    @Test
    public void testResolveContextVarsWithMap() {
        Map<String, String> map = new HashMap<>();
        map.put("env", "prod");
        assertThat(KeyMap.resolveContextVars("env={env}", map)).isEqualTo("env=prod");
    }

    @Test
    public void testResolveContextVarsPreservesUnresolved() {
        Map<String, String> map = new HashMap<>();
        assertThat(KeyMap.resolveContextVars("{nope}", map)).isEqualTo("{nope}");
    }

    // ---- resolveEnvVars ----

    @Test
    public void testResolveEnvVarsNoMatch() {
        assertThat(KeyMap.resolveEnvVars("plain text")).isEqualTo("plain text");
    }

    @Test
    public void testResolveEnvVarsUnknownVar() {
        String result = KeyMap.resolveEnvVars("${__NONEXISTENT_VAR_XYZ__}");
        // Should strip the ${} since preserveKeys=false in resolveEnvVars
        assertThat(result).isEqualTo("__NONEXISTENT_VAR_XYZ__");
    }

    // ---- getSystemVars ----

    @Test
    public void testGetSystemVarsNotNull() {
        assertThat(KeyMap.getSystemVars()).isNotNull();
        assertThat(KeyMap.getSystemVars()).isNotEmpty();
    }

    @Test
    public void testGetSystemVarsContainsSystemProps() {
        Map<Object, Object> vars = KeyMap.getSystemVars();
        // System properties should be present
        assertThat(vars.get("os.name")).isNotNull();
    }
}
