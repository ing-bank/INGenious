package com.ing.datalib.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.Test;

public class CurlParserTest {

    @Test
    public void detectsCurlPrefix() {
        assertThat(CurlParser.looksLikeCurl("curl https://example.com")).isTrue();
        assertThat(CurlParser.looksLikeCurl("  CURL -X POST https://example.com")).isTrue();
        assertThat(CurlParser.looksLikeCurl("curl\\\n  https://example.com")).isTrue();
        assertThat(CurlParser.looksLikeCurl("https://example.com")).isFalse();
        assertThat(CurlParser.looksLikeCurl("curling")).isFalse();
        assertThat(CurlParser.looksLikeCurl("")).isFalse();
        assertThat(CurlParser.looksLikeCurl(null)).isFalse();
        assertThat(CurlParser.looksLikeCurl("curl")).isFalse();
    }

    @Test
    public void parsesSimpleGet() {
        APIRequest r = CurlParser.parse("curl https://api.example.com/users");
        assertThat(r.getMethod()).isEqualTo(APIRequest.HttpMethod.GET);
        assertThat(r.getUrl()).isEqualTo("https://api.example.com/users");
        assertThat(r.getHeaders()).isEmpty();
        assertThat(r.getBody().getBodyType()).isEqualTo(RequestBody.BodyType.NONE);
    }

    @Test
    public void splitsQueryParameters() {
        APIRequest r = CurlParser.parse(
            "curl 'https://api.example.com/search?q=hello%20world&limit=10'"
        );
        assertThat(r.getUrl()).isEqualTo("https://api.example.com/search");
        assertThat(r.getQueryParams()).hasSize(2);
        assertThat(r.getQueryParams().get(0).getKey()).isEqualTo("q");
        assertThat(r.getQueryParams().get(0).getValue()).isEqualTo("hello world");
        assertThat(r.getQueryParams().get(1).getKey()).isEqualTo("limit");
        assertThat(r.getQueryParams().get(1).getValue()).isEqualTo("10");
    }

    @Test
    public void parsesHeadersAndJsonPostWithLineContinuations() {
        String cmd =
            "curl -X POST https://api.example.com/users \\\n" +
            "  -H 'Content-Type: application/json' \\\n" +
            "  -H \"X-Trace: abc-123\" \\\n" +
            "  --data-raw '{\"name\":\"Ada\",\"age\":36}'";
        APIRequest r = CurlParser.parse(cmd);
        assertThat(r.getMethod()).isEqualTo(APIRequest.HttpMethod.POST);
        assertThat(r.getUrl()).isEqualTo("https://api.example.com/users");
        assertThat(r.getHeaders()).hasSize(2);
        assertThat(r.getHeaders().get(0).getKey()).isEqualTo("Content-Type");
        assertThat(r.getHeaders().get(0).getValue()).isEqualTo("application/json");
        assertThat(r.getHeaders().get(1).getKey()).isEqualTo("X-Trace");
        assertThat(r.getHeaders().get(1).getValue()).isEqualTo("abc-123");
        assertThat(r.getBody().getBodyType()).isEqualTo(RequestBody.BodyType.RAW);
        assertThat(r.getBody().getRawFormat()).isEqualTo(RequestBody.RawFormat.JSON);
        assertThat(r.getBody().getRawContent()).isEqualTo("{\"name\":\"Ada\",\"age\":36}");
    }

    @Test
    public void inferPostWhenBodyPresentAndNoMethod() {
        APIRequest r = CurlParser.parse("curl https://api.example.com -d 'a=1'");
        assertThat(r.getMethod()).isEqualTo(APIRequest.HttpMethod.POST);
        assertThat(r.getBody().getRawContent()).isEqualTo("a=1");
    }

    @Test
    public void multipleDataFlagsAreJoinedWithAmpersand() {
        APIRequest r = CurlParser.parse("curl -X POST https://x.test -d 'a=1' --data 'b=2'");
        assertThat(r.getBody().getRawContent()).isEqualTo("a=1&b=2");
    }

    @Test
    public void parsesFormFlagsAsMultipart() {
        APIRequest r = CurlParser.parse(
            "curl -X POST https://x.test -F 'file=@/tmp/x.png' -F name=Ada"
        );
        assertThat(r.getBody().getBodyType()).isEqualTo(RequestBody.BodyType.FORM_DATA);
        assertThat(r.getBody().getFormData()).hasSize(2);
        assertThat(r.getBody().getFormData().get(0).getKey()).isEqualTo("file");
        assertThat(r.getBody().getFormData().get(0).getValue()).isEqualTo("@/tmp/x.png");
        assertThat(r.getBody().getFormData().get(1).getKey()).isEqualTo("name");
        assertThat(r.getBody().getFormData().get(1).getValue()).isEqualTo("Ada");
    }

    @Test
    public void parsesUrlEncodedFlags() {
        APIRequest r = CurlParser.parse(
            "curl https://x.test --data-urlencode 'q=hello world' --data-urlencode lang=en"
        );
        assertThat(r.getBody().getBodyType()).isEqualTo(RequestBody.BodyType.URL_ENCODED);
        assertThat(r.getBody().getUrlEncodedData()).hasSize(2);
        assertThat(r.getBody().getUrlEncodedData().get(0).getKey()).isEqualTo("q");
        assertThat(r.getBody().getUrlEncodedData().get(0).getValue()).isEqualTo("hello world");
    }

    @Test
    public void promotesBearerAuthorizationHeader() {
        APIRequest r = CurlParser.parse(
            "curl https://x.test -H 'Authorization: Bearer abc.def.ghi'"
        );
        assertThat(r.getAuth()).isNotNull();
        assertThat(r.getAuth().getAuthType()).isEqualTo(AuthConfig.AuthType.BEARER);
        assertThat(r.getAuth().getBearerToken()).isEqualTo("abc.def.ghi");
        assertThat(r.getHeaders()).isEmpty();
    }

    @Test
    public void parsesBasicAuthFromUserFlag() {
        APIRequest r = CurlParser.parse("curl -u alice:s3cret https://x.test");
        assertThat(r.getAuth().getAuthType()).isEqualTo(AuthConfig.AuthType.BASIC);
        assertThat(r.getAuth().getBasicUsername()).isEqualTo("alice");
        assertThat(r.getAuth().getBasicPassword()).isEqualTo("s3cret");
    }

    @Test
    public void respectsInsecureFlag() {
        APIRequest r = CurlParser.parse("curl -k https://x.test");
        assertThat(r.isSslVerificationEnabled()).isFalse();
    }

    @Test
    public void unknownFlagWithValueIsSkipped() {
        APIRequest r = CurlParser.parse("curl --max-time 10 https://x.test");
        assertThat(r.getUrl()).isEqualTo("https://x.test");
    }

    @Test
    public void emptyQueryStringDoesNotCreateParam() {
        APIRequest r = CurlParser.parse("curl https://x.test/path?");
        assertThat(r.getUrl()).isEqualTo("https://x.test/path");
        assertThat(r.getQueryParams()).isEmpty();
    }

    @Test
    public void noUrlYieldsNullUrl() {
        APIRequest r = CurlParser.parse("curl -X GET");
        assertThat(r.getMethod()).isEqualTo(APIRequest.HttpMethod.GET);
        assertThat(r.getUrl()).isNull();
    }

    @Test
    public void rejectsNonCurlInput() {
        assertThatThrownBy(() -> CurlParser.parse("wget https://x.test"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
