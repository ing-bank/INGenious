package com.ing.ingenious.api.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestMethodTypeTest {

    @Test
    void enumShouldExposeExpectedHttpMethods() {
        assertEquals(6, RequestMethodType.values().length);
        assertEquals(RequestMethodType.POST, RequestMethodType.valueOf("POST"));
        assertEquals(RequestMethodType.PUT, RequestMethodType.valueOf("PUT"));
        assertEquals(RequestMethodType.PATCH, RequestMethodType.valueOf("PATCH"));
        assertEquals(RequestMethodType.GET, RequestMethodType.valueOf("GET"));
        assertEquals(RequestMethodType.DELETE, RequestMethodType.valueOf("DELETE"));
        assertEquals(RequestMethodType.DELETEWITHPAYLOAD, RequestMethodType.valueOf("DELETEWITHPAYLOAD"));
    }
}
