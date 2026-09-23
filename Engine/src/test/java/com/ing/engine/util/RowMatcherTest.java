package com.ing.engine.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

/** Pure row-lookup tests for {@link RowMatcher} — no SAP GUI needed. */
public class RowMatcherTest {
    private static final List<String> ROWS = List.of(
        "Vendor Invoice",
        "Credit Memo",
        "Vendor Invoice 2"
    );

    @Test
    public void findRowReturnsFirstExactMatch() {
        int row = RowMatcher.findRow(
            ROWS.size(),
            ROWS::get,
            "Credit Memo",
            RowMatcher.MatchMode.EQUALS
        );
        assertThat(row).isEqualTo(1);
    }

    @Test
    public void findRowReturnsMinusOneWhenNoExactMatch() {
        int row = RowMatcher.findRow(
            ROWS.size(),
            ROWS::get,
            "Vendor Invoice ",
            RowMatcher.MatchMode.EQUALS
        );
        assertThat(row).isEqualTo(-1);
    }

    @Test
    public void findRowContainsModeMatchesSubstring() {
        int row = RowMatcher.findRow(
            ROWS.size(),
            ROWS::get,
            "Invoice",
            RowMatcher.MatchMode.CONTAINS
        );
        assertThat(row).isEqualTo(0);
    }

    @Test
    public void findRowReturnsMinusOneForEmptyTable() {
        int row = RowMatcher.findRow(0, ROWS::get, "anything", RowMatcher.MatchMode.EQUALS);
        assertThat(row).isEqualTo(-1);
    }

    @Test
    public void findRowTreatsNullCellAsNoMatch() {
        List<String> withNull = java.util.Arrays.asList("A", null, "B");
        int row = RowMatcher.findRow(
            withNull.size(),
            withNull::get,
            "B",
            RowMatcher.MatchMode.EQUALS
        );
        assertThat(row).isEqualTo(2);
    }
}
