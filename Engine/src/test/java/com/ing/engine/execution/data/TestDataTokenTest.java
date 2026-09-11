package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link TestDataToken} - the single parser/resolver for Test Data references and
 * embedded {@code {Sheet:Column}} tokens. Covers the "accept untagged and [Project]/[Shared]
 * tagged, braced or bare" contract.
 */
public class TestDataTokenTest {

    // ---- parse ---------------------------------------------------------------

    @Test
    public void parsesBareProjectReference() {
        assertThat(TestDataToken.parse("Basic:URL")).containsExactly("Basic", "URL");
        assertThat(TestDataToken.parse("{Basic:URL}")).containsExactly("Basic", "URL");
    }

    @Test
    public void parsesProjectTaggedReferenceKeepingTagOnSheet() {
        assertThat(TestDataToken.parse("[Project] Basic:URL"))
            .containsExactly("[Project] Basic", "URL");
        assertThat(TestDataToken.parse("{[Project] Basic:URL}"))
            .containsExactly("[Project] Basic", "URL");
    }

    @Test
    public void parsesSharedTaggedReference() {
        assertThat(TestDataToken.parse("{[Shared] TestData0:Data1}"))
            .containsExactly("[Shared] TestData0", "Data1");
    }

    @Test
    public void toleratesWhitespaceAndSplitsOnFirstColon() {
        assertThat(TestDataToken.parse("  { [Shared] Sheet A : a:b } "))
            .containsExactly("[Shared] Sheet A", "a:b");
    }

    @Test
    public void rejectsMalformedReferences() {
        assertThat(TestDataToken.parse(null)).isNull();
        assertThat(TestDataToken.parse("Basic")).isNull();
        assertThat(TestDataToken.parse("{Basic}")).isNull();
        assertThat(TestDataToken.parse(":URL")).isNull();
        assertThat(TestDataToken.parse("Basic:")).isNull();
        assertThat(TestDataToken.parse("{[Project] :URL}")).isNull();
        assertThat(TestDataToken.parse("{[Shared] TestData0:}")).isNull();
    }

    // ---- scopeTag ----------------------------------------------------------

    @Test
    public void reportsScopeTag() {
        assertThat(TestDataToken.scopeTag("Basic:URL")).isEmpty();
        assertThat(TestDataToken.scopeTag("{Basic:URL}")).isEmpty();
        assertThat(TestDataToken.scopeTag("[Project] Basic:URL")).isEqualTo("[Project]");
        assertThat(TestDataToken.scopeTag("{[Shared] X:Y}")).isEqualTo("[Shared]");
        assertThat(TestDataToken.hasScopeTag("X:Y")).isFalse();
        assertThat(TestDataToken.hasScopeTag("[Project] X:Y")).isTrue();
    }

    // ---- isReference -----------------------------------------------------

    @Test
    public void recognisesWholeInputReferences() {
        assertThat(TestDataToken.isReference("Basic:URL")).isTrue();
        assertThat(TestDataToken.isReference("{Basic:URL}")).isTrue();
        assertThat(TestDataToken.isReference("[Project] Basic:URL")).isTrue();
        assertThat(TestDataToken.isReference("{[Shared] TestData0:Data1}")).isTrue();
    }

    @Test
    public void doesNotMistakeUrlsOrTimesForReferences() {
        assertThat(TestDataToken.isReference("http://host:8080/x")).isFalse();
        assertThat(TestDataToken.isReference("12:30")).isFalse();
        assertThat(TestDataToken.isReference("@literal")).isFalse();
    }

    // ---- resolveEmbeddedTokens -------------------------------------------

    private UserDataAccess userDataReturning(String sheet, String column, String value) {
        UserDataAccess ud = mock(UserDataAccess.class);
        when(ud.getData(sheet, column)).thenReturn(value);
        return ud;
    }

    @Test
    public void substitutesBareAndTaggedTokensInAPayload() {
        UserDataAccess ud = mock(UserDataAccess.class);
        when(ud.getData("ApiData", "UserId")).thenReturn("42");
        when(ud.getData("[Project] ApiData", "Name")).thenReturn("alice");
        when(ud.getData("[Shared] Common", "Token")).thenReturn("t-123");

        String body =
            "{\"id\":{ApiData:UserId},\"name\":\"{[Project] ApiData:Name}\",\"tok\":\"{[Shared] Common:Token}\"}";

        assertThat(TestDataToken.resolveEmbeddedTokens(body, ud))
            .isEqualTo("{\"id\":42,\"name\":\"alice\",\"tok\":\"t-123\"}");
    }

    @Test
    public void leavesJsonObjectLiteralsUntouched() {
        UserDataAccess ud = mock(UserDataAccess.class);
        String json = "{\"a\":\"b\",\"c\":{\"d\":\"e\"}}";
        assertThat(TestDataToken.resolveEmbeddedTokens(json, ud)).isEqualTo(json);
    }

    @Test
    public void leavesUnresolvedTokensLiteral() {
        UserDataAccess ud = mock(UserDataAccess.class);
        when(ud.getData("Known", "Col")).thenReturn("v");
        when(ud.getData("Unknown", "Col"))
            .thenThrow(new com.ing.engine.execution.exception.data.DataNotFoundException("nope"));

        assertThat(TestDataToken.resolveEmbeddedTokens("a={Known:Col} b={Unknown:Col}", ud))
            .isEqualTo("a=v b={Unknown:Col}");
    }

    @Test
    public void noBraceMeansNoWork() {
        UserDataAccess ud = mock(UserDataAccess.class);
        assertThat(TestDataToken.resolveEmbeddedTokens("plain text", ud)).isEqualTo("plain text");
        assertThat(TestDataToken.resolveEmbeddedTokens(null, ud)).isNull();
    }
}
