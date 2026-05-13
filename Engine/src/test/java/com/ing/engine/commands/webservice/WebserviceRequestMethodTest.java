package com.ing.engine.commands.webservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ingenious.api.types.RequestMethodType;
import org.testng.annotations.Test;

/**
 * Tests for the Webservice.RequestMethodType enum.
 */
public class WebserviceRequestMethodTest {

    @Test
    public void testAllMethodsExist() {
        RequestMethodType[] methods = RequestMethodType.values();
        assertThat(methods).hasSize(6);
    }

    @Test
    public void testPostValue() {
        assertThat(RequestMethodType.valueOf("POST")).isEqualTo(RequestMethodType.POST);
    }

    @Test
    public void testPutValue() {
        assertThat(RequestMethodType.valueOf("PUT")).isEqualTo(RequestMethodType.PUT);
    }

    @Test
    public void testPatchValue() {
        assertThat(RequestMethodType.valueOf("PATCH")).isEqualTo(RequestMethodType.PATCH);
    }

    @Test
    public void testGetValue() {
        assertThat(RequestMethodType.valueOf("GET")).isEqualTo(RequestMethodType.GET);
    }

    @Test
    public void testDeleteValue() {
        assertThat(RequestMethodType.valueOf("DELETE")).isEqualTo(RequestMethodType.DELETE);
    }

    @Test
    public void testDeleteWithPayloadValue() {
        assertThat(RequestMethodType.valueOf("DELETEWITHPAYLOAD")).isEqualTo(RequestMethodType.DELETEWITHPAYLOAD);
    }

    @Test
    public void testOrdinalOrder() {
        assertThat(RequestMethodType.POST.ordinal()).isEqualTo(0);
        assertThat(RequestMethodType.PUT.ordinal()).isEqualTo(1);
        assertThat(RequestMethodType.PATCH.ordinal()).isEqualTo(2);
        assertThat(RequestMethodType.GET.ordinal()).isEqualTo(3);
        assertThat(RequestMethodType.DELETE.ordinal()).isEqualTo(4);
        assertThat(RequestMethodType.DELETEWITHPAYLOAD.ordinal()).isEqualTo(5);
    }

    @Test
    public void testToString() {
        assertThat(RequestMethodType.POST.toString()).isEqualTo("POST");
        assertThat(RequestMethodType.DELETEWITHPAYLOAD.toString()).isEqualTo("DELETEWITHPAYLOAD");
    }
}
