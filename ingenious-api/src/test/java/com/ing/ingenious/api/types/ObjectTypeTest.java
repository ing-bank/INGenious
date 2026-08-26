package com.ing.ingenious.api.types;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectTypeTest {

    @Test
    void getObjectTypesShouldContainAllInitialTypes() {
        Set<String> objectTypes = ObjectType.getObjectTypes();

        assertEquals(18, objectTypes.size());
        assertTrue(objectTypes.contains(ObjectType.BROWSER));
        assertTrue(objectTypes.contains(ObjectType.WEB));
        assertTrue(objectTypes.contains(ObjectType.MOBILE));
        assertTrue(objectTypes.contains(ObjectType.IMAGE));
        assertTrue(objectTypes.contains(ObjectType.PLAYWRIGHT));
        assertTrue(objectTypes.contains(ObjectType.APP));
        assertTrue(objectTypes.contains(ObjectType.DATABASE));
        assertTrue(objectTypes.contains(ObjectType.PROTRACTORJS));
        assertTrue(objectTypes.contains(ObjectType.ANY));
        assertTrue(objectTypes.contains(ObjectType.WEBSERVICE));
        assertTrue(objectTypes.contains(ObjectType.FILE));
        assertTrue(objectTypes.contains(ObjectType.KAFKA));
        assertTrue(objectTypes.contains(ObjectType.QUEUE));
        assertTrue(objectTypes.contains(ObjectType.DATA));
        assertTrue(objectTypes.contains(ObjectType.GENERAL));
        assertTrue(objectTypes.contains(ObjectType.STRINGOPERATIONS));
        assertTrue(objectTypes.contains(ObjectType.STRUCTUREDDATA));
        assertTrue(objectTypes.contains(ObjectType.SAP));
    }

    @Test
    void getObjectTypesShouldReturnDefensiveCopy() {
        Set<String> first = ObjectType.getObjectTypes();
        first.remove(ObjectType.BROWSER);
        first.add("CustomType");

        Set<String> second = ObjectType.getObjectTypes();

        assertTrue(second.contains(ObjectType.BROWSER));
        assertFalse(second.contains("CustomType"));
    }
}
