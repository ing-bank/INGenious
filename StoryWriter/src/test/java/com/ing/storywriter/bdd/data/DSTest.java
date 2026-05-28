package com.ing.storywriter.bdd.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DSTest {

    @BeforeMethod
    public void setUp() {
        DS.STEPDIC.clear();
        DS.VARSET.clear();
    }

    // ---- Constants ----

    @Test
    public void testStepTypes() {
        assertThat(DS.STEPTYPES).containsExactly("Given", "And", "When", "Then", "But");
    }

    @Test
    public void testConstants() {
        assertThat(DS.LN).isEqualTo("\n");
        assertThat(DS.TAB).isEqualTo("\t");
        assertThat(DS.GSTORIES).isEqualTo("GivenStories");
        assertThat(DS.META).isEqualTo("Meta");
    }

    // ---- update ----

    @Test
    public void testUpdateAddsToStepDic() {
        DS.update("login to app");
        assertThat(DS.STEPDIC).contains("login to app");
    }

    @Test
    public void testUpdateSkipsShortStrings() {
        DS.update("ab");
        assertThat(DS.STEPDIC).isEmpty();
    }

    @Test
    public void testUpdateSkipsNull() {
        DS.update((String) null);
        assertThat(DS.STEPDIC).isEmpty();
    }

    @Test
    public void testUpdateAddsVariableToVarSet() {
        DS.update("<username>");
        assertThat(DS.VARSET).contains("<username>");
        assertThat(DS.STEPDIC).doesNotContain("<username>");
    }

    @Test
    public void testUpdateMultipleValues() {
        DS.update("step one", "step two", "<var1>");
        assertThat(DS.STEPDIC).containsExactlyInAnyOrder("step one", "step two");
        assertThat(DS.VARSET).contains("<var1>");
    }

    @Test
    public void testUpdateNullArray() {
        DS.update((String[]) null);
        // Should not throw
        assertThat(DS.STEPDIC).isEmpty();
    }

    @Test
    public void testUpdateRespectsLimit() {
        // Fill up to 500 entries
        for (int i = 0; i < 500; i++) {
            DS.update("step " + i + " text");
        }
        int sizeBefore = DS.STEPDIC.size();
        DS.update("extra step text");
        assertThat(DS.STEPDIC.size()).isEqualTo(sizeBefore); // Should not grow beyond 500
    }

    // ---- updateV ----

    @Test
    public void testUpdateVAddsWithBrackets() {
        DS.updateV("myvar");
        assertThat(DS.VARSET).contains("<myvar>");
    }

    @Test
    public void testUpdateVSkipsShortStrings() {
        DS.updateV("ab");
        assertThat(DS.VARSET).isEmpty();
    }

    @Test
    public void testUpdateVMultiple() {
        DS.updateV("var1", "var2");
        assertThat(DS.VARSET).containsExactlyInAnyOrder("<var1>", "<var2>");
    }
}
