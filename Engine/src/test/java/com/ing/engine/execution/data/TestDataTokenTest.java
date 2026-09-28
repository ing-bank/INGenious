package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link TestDataToken} - the single parser/resolver for Test Data references and
 * embedded {@code {Sheet:Column}} tokens. Covers the "accept untagged and trailing
 * @Project/@Shared tagged, braced or bare" contract.
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
        assertThat(TestDataToken.parse("Basic:URL@Project"))
            .containsExactly("Basic@Project", "URL");
        assertThat(TestDataToken.parse("{Basic:URL@Project}"))
            .containsExactly("Basic@Project", "URL");
    }

    @Test
    public void parsesSharedTaggedReference() {
        assertThat(TestDataToken.parse("{TestData0:Data1@Shared}"))
            .containsExactly("TestData0@Shared", "Data1");
    }

    @Test
    public void toleratesWhitespaceAndSplitsOnFirstColon() {
        assertThat(TestDataToken.parse("  { Sheet A : a:b@Shared } "))
            .containsExactly("Sheet A@Shared", "a:b");
    }

    @Test
    public void rejectsMalformedReferences() {
        assertThat(TestDataToken.parse(null)).isNull();
        assertThat(TestDataToken.parse("Basic")).isNull();
        assertThat(TestDataToken.parse("{Basic}")).isNull();
        assertThat(TestDataToken.parse(":URL")).isNull();
        assertThat(TestDataToken.parse("Basic:")).isNull();
        assertThat(TestDataToken.parse("{:URL@Project}")).isNull();
        assertThat(TestDataToken.parse("{TestData0:@Shared}")).isNull();
    }

    // ---- scopeTag ----------------------------------------------------------

    @Test
    public void reportsScopeTag() {
        assertThat(TestDataToken.scopeTag("Basic:URL")).isEmpty();
        assertThat(TestDataToken.scopeTag("{Basic:URL}")).isEmpty();
        assertThat(TestDataToken.scopeTag("Basic:URL@Project")).isEqualTo("@Project");
        assertThat(TestDataToken.scopeTag("{X:Y@Shared}")).isEqualTo("@Shared");
        assertThat(TestDataToken.hasScopeTag("X:Y")).isFalse();
        assertThat(TestDataToken.hasScopeTag("X:Y@Project")).isTrue();
    }

    // ---- isReference -----------------------------------------------------

    @Test
    public void recognisesWholeInputReferences() {
        assertThat(TestDataToken.isReference("Basic:URL")).isTrue();
        assertThat(TestDataToken.isReference("{Basic:URL}")).isTrue();
        assertThat(TestDataToken.isReference("Basic:URL@Project")).isTrue();
        assertThat(TestDataToken.isReference("{TestData0:Data1@Shared}")).isTrue();
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
        when(ud.getData("ApiData@Project", "Name")).thenReturn("alice");
        when(ud.getData("Common@Shared", "Token")).thenReturn("t-123");

        String body =
            "{\"id\":{ApiData:UserId},\"name\":\"{ApiData:Name@Project}\",\"tok\":\"{Common:Token@Shared}\"}";

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
