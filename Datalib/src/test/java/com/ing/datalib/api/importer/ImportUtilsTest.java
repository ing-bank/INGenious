package com.ing.datalib.api.importer;

import static org.testng.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.Test;

/**
 * Tests for {@link ImportUtils}.
 */
public class ImportUtilsTest {

    @Test
    public void testRewriteVariables_mustacheToPercent() {
        String input = "{{baseUrl}}/api/{{version}}/users";
        String expected = "%baseUrl%/api/%version%/users";
        assertEquals(ImportUtils.rewriteVariables(input), expected);
    }

    @Test
    public void testRewriteVariables_withWhitespace() {
        String input = "{{ baseUrl }}/api/{{ version }}/users";
        String expected = "%baseUrl%/api/%version%/users";
        assertEquals(ImportUtils.rewriteVariables(input), expected);
    }

    @Test
    public void testRewriteVariables_nullInput() {
        assertNull(ImportUtils.rewriteVariables(null));
    }

    @Test
    public void testRewriteVariables_emptyInput() {
        assertEquals(ImportUtils.rewriteVariables(""), "");
    }

    @Test
    public void testConvertToDatasheetSyntax_percentVariables() {
        Set<String> knownVars = new HashSet<>();
        knownVars.add("BaseUrl");
        knownVars.add("UserName");

        String input = "%BaseUrl%/api/%UserName%/profile";
        String expected = "{Customer_APIs:BaseUrl}/api/{Customer_APIs:UserName}/profile";
        assertEquals(
            ImportUtils.convertToDatasheetSyntax(input, "Customer_APIs", knownVars),
            expected
        );
    }

    @Test
    public void testConvertToDatasheetSyntax_mustacheVariables() {
        Set<String> knownVars = new HashSet<>();
        knownVars.add("BaseUrl");
        knownVars.add("UserName");

        String input = "{{BaseUrl}}/api/{{UserName}}/profile";
        String expected = "{Customer_APIs:BaseUrl}/api/{Customer_APIs:UserName}/profile";
        assertEquals(
            ImportUtils.convertToDatasheetSyntax(input, "Customer_APIs", knownVars),
            expected
        );
    }

    @Test
    public void testConvertToDatasheetSyntax_mixedVariables() {
        Set<String> knownVars = new HashSet<>();
        knownVars.add("BaseUrl");
        knownVars.add("Token");

        String input = "{{BaseUrl}}/api/auth?token=%Token%";
        String expected = "{MySheet:BaseUrl}/api/auth?token={MySheet:Token}";
        assertEquals(ImportUtils.convertToDatasheetSyntax(input, "MySheet", knownVars), expected);
    }

    @Test
    public void testConvertToDatasheetSyntax_unknownVariableUnchanged() {
        Set<String> knownVars = new HashSet<>();
        knownVars.add("BaseUrl");

        String input = "%BaseUrl%/api/%Unknown%/data";
        String expected = "{Sheet:BaseUrl}/api/%Unknown%/data";
        assertEquals(ImportUtils.convertToDatasheetSyntax(input, "Sheet", knownVars), expected);
    }

    @Test
    public void testConvertToDatasheetSyntax_nullInput() {
        Set<String> knownVars = new HashSet<>();
        knownVars.add("BaseUrl");

        assertNull(ImportUtils.convertToDatasheetSyntax(null, "Sheet", knownVars));
    }

    @Test
    public void testConvertToDatasheetSyntax_emptyInput() {
        Set<String> knownVars = new HashSet<>();
        knownVars.add("BaseUrl");

        assertEquals(ImportUtils.convertToDatasheetSyntax("", "Sheet", knownVars), "");
    }

    @Test
    public void testConvertToDatasheetSyntax_nullKnownVars() {
        String input = "%BaseUrl%/api";
        assertEquals(ImportUtils.convertToDatasheetSyntax(input, "Sheet", null), input);
    }

    @Test
    public void testConvertToDatasheetSyntax_emptyKnownVars() {
        Set<String> knownVars = new HashSet<>();

        String input = "%BaseUrl%/api";
        assertEquals(ImportUtils.convertToDatasheetSyntax(input, "Sheet", knownVars), input);
    }

    @Test
    public void testConvertToDatasheetSyntax_nullDatasheetName() {
        Set<String> knownVars = new HashSet<>();
        knownVars.add("BaseUrl");

        String input = "%BaseUrl%/api";
        assertEquals(ImportUtils.convertToDatasheetSyntax(input, null, knownVars), input);
    }

    @Test
    public void testSanitizeFileName_validName() {
        assertEquals(ImportUtils.sanitizeFileName("MyFile"), "MyFile");
    }

    @Test
    public void testSanitizeFileName_withSpaces() {
        assertEquals(ImportUtils.sanitizeFileName("My File Name"), "My_File_Name");
    }

    @Test
    public void testSanitizeFileName_withSpecialChars() {
        assertEquals(ImportUtils.sanitizeFileName("Customer APIs (v2)"), "Customer_APIs__v2_");
    }

    @Test
    public void testSanitizeFileName_nullInput() {
        assertEquals(ImportUtils.sanitizeFileName(null), "unnamed");
    }

    @Test
    public void testSanitizeFileName_emptyInput() {
        assertEquals(ImportUtils.sanitizeFileName(""), "unnamed");
    }
}
