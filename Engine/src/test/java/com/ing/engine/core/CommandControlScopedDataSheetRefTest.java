package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link CommandControl#parseScopedDataSheetRef(String)} - the parsing that lets
 * the String Operations {@code {sheet:column}} test-data pattern carry an explicit trailing
 * {@code @Shared} / {@code @Project} scope tag.
 */
public class CommandControlScopedDataSheetRefTest {

    @Test
    public void bracedSharedRefKeepsTagOnSheetAndSplitsColumn() {
        assertThat(CommandControl.parseScopedDataSheetRef("{TestData0:Data1@Shared}"))
            .containsExactly("TestData0@Shared", "Data1");
    }

    @Test
    public void bareSharedRefIsAlsoAccepted() {
        assertThat(CommandControl.parseScopedDataSheetRef("TestData0:Data1@Shared"))
            .containsExactly("TestData0@Shared", "Data1");
    }

    @Test
    public void projectTagIsHandledTheSameWay() {
        assertThat(CommandControl.parseScopedDataSheetRef("{Basic:URL@Project}"))
            .containsExactly("Basic@Project", "URL");
    }

    @Test
    public void surroundingWhitespaceIsTolerated() {
        assertThat(CommandControl.parseScopedDataSheetRef("  { Sheet A : Col B@Shared } "))
            .containsExactly("Sheet A@Shared", "Col B");
    }

    @Test
    public void onlyTheFirstColonSeparatesSheetFromColumn() {
        assertThat(CommandControl.parseScopedDataSheetRef("{Sheet:a:b@Shared}"))
            .containsExactly("Sheet@Shared", "a:b");
    }

    @Test
    public void untaggedReferenceReturnsNullSoTheLegacyPathHandlesIt() {
        assertThat(CommandControl.parseScopedDataSheetRef("{TestData0:Data1}")).isNull();
        assertThat(CommandControl.parseScopedDataSheetRef("TestData0:Data1")).isNull();
    }

    @Test
    public void malformedScopedReferencesReturnNull() {
        assertThat(CommandControl.parseScopedDataSheetRef("{TestData0@Shared}")).isNull();
        assertThat(CommandControl.parseScopedDataSheetRef("{:Data1@Shared}")).isNull();
        assertThat(CommandControl.parseScopedDataSheetRef("{TestData0:@Shared}")).isNull();
        assertThat(CommandControl.parseScopedDataSheetRef(null)).isNull();
    }
}
