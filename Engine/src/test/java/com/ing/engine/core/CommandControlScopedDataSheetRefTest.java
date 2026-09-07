package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link CommandControl#parseScopedDataSheetRef(String)} - the parsing that lets
 * the String Operations {@code {sheet:column}} test-data pattern carry an explicit
 * {@code [Shared]} / {@code [Project]} scope tag.
 */
public class CommandControlScopedDataSheetRefTest {

    @Test
    public void bracedSharedRefKeepsTagOnSheetAndSplitsColumn() {
        assertThat(CommandControl.parseScopedDataSheetRef("{[Shared] TestData0:Data1}"))
            .containsExactly("[Shared] TestData0", "Data1");
    }

    @Test
    public void bareSharedRefIsAlsoAccepted() {
        assertThat(CommandControl.parseScopedDataSheetRef("[Shared] TestData0:Data1"))
            .containsExactly("[Shared] TestData0", "Data1");
    }

    @Test
    public void projectTagIsHandledTheSameWay() {
        assertThat(CommandControl.parseScopedDataSheetRef("{[Project] Basic:URL}"))
            .containsExactly("[Project] Basic", "URL");
    }

    @Test
    public void surroundingWhitespaceIsTolerated() {
        assertThat(CommandControl.parseScopedDataSheetRef("  { [Shared] Sheet A : Col B } "))
            .containsExactly("[Shared] Sheet A", "Col B");
    }

    @Test
    public void onlyTheFirstColonSeparatesSheetFromColumn() {
        assertThat(CommandControl.parseScopedDataSheetRef("{[Shared] Sheet:a:b}"))
            .containsExactly("[Shared] Sheet", "a:b");
    }

    @Test
    public void untaggedReferenceReturnsNullSoTheLegacyPathHandlesIt() {
        assertThat(CommandControl.parseScopedDataSheetRef("{TestData0:Data1}")).isNull();
        assertThat(CommandControl.parseScopedDataSheetRef("TestData0:Data1")).isNull();
    }

    @Test
    public void malformedScopedReferencesReturnNull() {
        assertThat(CommandControl.parseScopedDataSheetRef("{[Shared] TestData0}")).isNull();
        assertThat(CommandControl.parseScopedDataSheetRef("{[Shared] :Data1}")).isNull();
        assertThat(CommandControl.parseScopedDataSheetRef("{[Shared] TestData0:}")).isNull();
        assertThat(CommandControl.parseScopedDataSheetRef(null)).isNull();
    }
}
