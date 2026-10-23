package com.ing.ide.util;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Regression test for ING-reported unrelated finding: "Users are allowed to rename any sheet to
 * GlobalData" - GlobalData.csv is a reserved file every Test Data provider excludes from its
 * normal sheet listing, so a datasheet renamed to that name silently disappears on reload.
 */
public class ValidatorTest {

    @Test
    public void isValidTestDataSheetName_RejectsReservedGlobalDataName() {
        assertFalse(Validator.isValidTestDataSheetName("GlobalData"));
    }

    @Test
    public void isValidTestDataSheetName_RejectsReservedGlobalDataNameCaseInsensitive() {
        assertFalse(Validator.isValidTestDataSheetName("globaldata"));
        assertFalse(Validator.isValidTestDataSheetName("GLOBALDATA"));
        assertFalse(Validator.isValidTestDataSheetName("GlObAlDaTa"));
    }

    @Test
    public void isValidTestDataSheetName_AcceptsOrdinaryNames() {
        assertTrue(Validator.isValidTestDataSheetName("ProjectSheet"));
        assertTrue(Validator.isValidTestDataSheetName("TestData0"));
        assertTrue(Validator.isValidTestDataSheetName("Global_Data"));
        assertTrue(Validator.isValidTestDataSheetName("GlobalDataSheet"));
    }

    @Test
    public void isValidTestDataSheetName_StillEnforcesBaseCharacterRules() {
        assertFalse(Validator.isValidTestDataSheetName("Invalid Name"));
        assertFalse(Validator.isValidTestDataSheetName(null));
    }

    @Test
    public void isValidTestDataName_StillAllowsGlobalDataForColumnNames() {
        // The reserved-name rule only applies to datasheet names, not column names -
        // a column literally named "GlobalData" doesn't collide with GlobalData.csv.
        assertTrue(Validator.isValidTestDataName("GlobalData"));
    }
}
