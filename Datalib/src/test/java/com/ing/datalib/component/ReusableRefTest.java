package com.ing.datalib.component;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Tests for ReusableRef parser covering scoped and unscoped reference formats.
 */
public class ReusableRefTest {

    @Test
    public void testParseProjectScoped() {
        ReusableRef ref = ReusableRef.parse("[Project] LoginScenario:LoginTest");
        assertEquals(ref.getScope(), ReusableRef.Scope.PROJECT);
        assertEquals(ref.getScenarioName(), "LoginScenario");
        assertEquals(ref.getTestCaseName(), "LoginTest");
        assertTrue(ref.isScoped());
    }

    @Test
    public void testParseSharedScoped() {
        ReusableRef ref = ReusableRef.parse("[Shared] CommonScenario:CommonTest");
        assertEquals(ref.getScope(), ReusableRef.Scope.SHARED);
        assertEquals(ref.getScenarioName(), "CommonScenario");
        assertEquals(ref.getTestCaseName(), "CommonTest");
        assertTrue(ref.isScoped());
    }

    @Test
    public void testParseUnscoped() {
        ReusableRef ref = ReusableRef.parse("LoginScenario:LoginTest");
        assertEquals(ref.getScope(), ReusableRef.Scope.UNSCOPED);
        assertEquals(ref.getScenarioName(), "LoginScenario");
        assertEquals(ref.getTestCaseName(), "LoginTest");
        assertFalse(ref.isScoped());
    }

    @Test
    public void testParseWithWhitespace() {
        ReusableRef ref = ReusableRef.parse("  [Project] LoginScenario : LoginTest  ");
        assertEquals(ref.getScope(), ReusableRef.Scope.PROJECT);
        assertEquals(ref.getScenarioName(), "LoginScenario");
        assertEquals(ref.getTestCaseName(), "LoginTest");
    }

    @Test
    public void testParseSharedScoped_CaseInsensitive() {
        ReusableRef ref = ReusableRef.parse("[shared] CommonScenario:CommonTest");
        assertEquals(ref.getScope(), ReusableRef.Scope.SHARED);
    }

    @Test
    public void testFormat_ProjectScoped() {
        ReusableRef ref = new ReusableRef(ReusableRef.Scope.PROJECT, "LoginScenario", "LoginTest");
        assertEquals(ref.format(), "[Project] LoginScenario:LoginTest");
    }

    @Test
    public void testFormat_SharedScoped() {
        ReusableRef ref = new ReusableRef(ReusableRef.Scope.SHARED, "CommonScenario", "CommonTest");
        assertEquals(ref.format(), "[Shared] CommonScenario:CommonTest");
    }

    @Test
    public void testFormat_Unscoped() {
        ReusableRef ref = new ReusableRef(ReusableRef.Scope.UNSCOPED, "LoginScenario", "LoginTest");
        assertEquals(ref.format(), "LoginScenario:LoginTest");
    }

    @Test
    public void testFormatCanonical_Unscoped() {
        ReusableRef ref = new ReusableRef(ReusableRef.Scope.UNSCOPED, "LoginScenario", "LoginTest");
        assertEquals(ref.formatCanonical(), "[Project] LoginScenario:LoginTest");
    }

    @Test
    public void testFormatCanonical_ProjectScoped() {
        ReusableRef ref = new ReusableRef(ReusableRef.Scope.PROJECT, "LoginScenario", "LoginTest");
        assertEquals(ref.formatCanonical(), "[Project] LoginScenario:LoginTest");
    }

    @Test
    public void testFormatProjectScopedStatic() {
        String formatted = ReusableRef.formatProjectScoped("LoginScenario", "LoginTest");
        assertEquals(formatted, "[Project] LoginScenario:LoginTest");
    }

    @Test
    public void testFormatSharedScopedStatic() {
        String formatted = ReusableRef.formatSharedScoped("CommonScenario", "CommonTest");
        assertEquals(formatted, "[Shared] CommonScenario:CommonTest");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseInvalid_NoColon() {
        ReusableRef.parse("[Project] LoginScenario");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseInvalid_EmptyScenario() {
        ReusableRef.parse("[Project] :LoginTest");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseInvalid_EmptyTestCase() {
        ReusableRef.parse("[Project] LoginScenario:");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseInvalid_UnknownScope() {
        ReusableRef.parse("[Unknown] LoginScenario:LoginTest");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseInvalid_Null() {
        ReusableRef.parse(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseInvalid_Empty() {
        ReusableRef.parse("");
    }

    @Test
    public void testRoundTrip_ProjectScoped() {
        String original = "[Project] LoginScenario:LoginTest";
        ReusableRef ref = ReusableRef.parse(original);
        assertEquals(ref.format(), original);
    }

    @Test
    public void testRoundTrip_SharedScoped() {
        String original = "[Shared] CommonScenario:CommonTest";
        ReusableRef ref = ReusableRef.parse(original);
        assertEquals(ref.format(), original);
    }

    @Test
    public void testRoundTrip_Unscoped() {
        String original = "LoginScenario:LoginTest";
        ReusableRef ref = ReusableRef.parse(original);
        assertEquals(ref.format(), original);
    }

    @Test
    public void testEquality_SameContent() {
        ReusableRef ref1 = new ReusableRef(ReusableRef.Scope.PROJECT, "LoginScenario", "LoginTest");
        ReusableRef ref2 = new ReusableRef(ReusableRef.Scope.PROJECT, "LoginScenario", "LoginTest");
        assertEquals(ref1, ref2);
    }

    @Test
    public void testEquality_DifferentScope() {
        ReusableRef ref1 = new ReusableRef(ReusableRef.Scope.PROJECT, "LoginScenario", "LoginTest");
        ReusableRef ref2 = new ReusableRef(ReusableRef.Scope.SHARED, "LoginScenario", "LoginTest");
        assertNotEquals(ref1, ref2);
    }

    @Test
    public void testEquality_CaseInsensitive() {
        ReusableRef ref1 = new ReusableRef(ReusableRef.Scope.PROJECT, "LoginScenario", "LoginTest");
        ReusableRef ref2 = new ReusableRef(ReusableRef.Scope.PROJECT, "loginscenario", "logintest");
        assertEquals(ref1, ref2);
    }

    @Test
    public void testToString() {
        ReusableRef ref = new ReusableRef(ReusableRef.Scope.PROJECT, "LoginScenario", "LoginTest");
        assertEquals(ref.toString(), "[Project] LoginScenario:LoginTest");
    }

    @Test
    public void testHashCode_Consistency() {
        ReusableRef ref1 = ReusableRef.parse("[Project] LoginScenario:LoginTest");
        ReusableRef ref2 = ReusableRef.parse("[Project] LoginScenario:LoginTest");
        assertEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    public void testHashCode_CaseInsensitive() {
        ReusableRef ref1 = new ReusableRef(ReusableRef.Scope.PROJECT, "LoginScenario", "LoginTest");
        ReusableRef ref2 = new ReusableRef(ReusableRef.Scope.PROJECT, "LOGINSCENARIO", "LOGINTEST");
        assertEquals(ref1.hashCode(), ref2.hashCode());
    }
}
