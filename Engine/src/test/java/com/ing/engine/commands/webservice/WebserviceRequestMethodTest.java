package com.ing.engine.commands.webservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ingenious.api.types.RequestMethod;
import org.testng.annotations.Test;

/**
 * Tests for the Webservice.RequestMethod enum.
 */
public class WebserviceRequestMethodTest {

    @Test
    public void testAllMethodsExist() {
        RequestMethod[] methods = RequestMethod.values();
        assertThat(methods).hasSize(6);
    }

    @Test
    public void testPostValue() {
        assertThat(RequestMethod.valueOf("POST")).isEqualTo(RequestMethod.POST);
    }

    @Test
    public void testPutValue() {
        assertThat(RequestMethod.valueOf("PUT")).isEqualTo(RequestMethod.PUT);
    }

    @Test
    public void testPatchValue() {
        assertThat(RequestMethod.valueOf("PATCH")).isEqualTo(RequestMethod.PATCH);
    }

    @Test
    public void testGetValue() {
        assertThat(RequestMethod.valueOf("GET")).isEqualTo(RequestMethod.GET);
    }

    @Test
    public void testDeleteValue() {
        assertThat(RequestMethod.valueOf("DELETE")).isEqualTo(RequestMethod.DELETE);
    }

    @Test
    public void testDeleteWithPayloadValue() {
        assertThat(RequestMethod.valueOf("DELETEWITHPAYLOAD")).isEqualTo(RequestMethod.DELETEWITHPAYLOAD);
    }

    @Test
    public void testOrdinalOrder() {
        assertThat(RequestMethod.POST.ordinal()).isEqualTo(0);
        assertThat(RequestMethod.PUT.ordinal()).isEqualTo(1);
        assertThat(RequestMethod.PATCH.ordinal()).isEqualTo(2);
        assertThat(RequestMethod.GET.ordinal()).isEqualTo(3);
        assertThat(RequestMethod.DELETE.ordinal()).isEqualTo(4);
        assertThat(RequestMethod.DELETEWITHPAYLOAD.ordinal()).isEqualTo(5);
    }

    @Test
    public void testToString() {
        assertThat(RequestMethod.POST.toString()).isEqualTo("POST");
        assertThat(RequestMethod.DELETEWITHPAYLOAD.toString()).isEqualTo("DELETEWITHPAYLOAD");
    }
}
