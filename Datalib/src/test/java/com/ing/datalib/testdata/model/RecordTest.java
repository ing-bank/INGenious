package com.ing.datalib.testdata.model;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Tests for {@link Record}.
 */
public class RecordTest {

    @Test
    public void testRecordInitialization() {
        Record record = new Record();
        assertEquals(record.size(), Record.HEADERS.length);
        assertEquals(record.getScenario(), "");
        assertEquals(record.getTestcase(), "");
        assertEquals(record.getScope(), "");
        assertEquals(record.getIteration(), "");
        assertEquals(record.getSubIteration(), "");
    }

    @Test
    public void testSetAndGetScenario() {
        Record record = new Record();
        record.setScenario("Customer APIs");
        assertEquals(record.getScenario(), "Customer APIs");
    }

    @Test
    public void testSetAndGetTestcase() {
        Record record = new Record();
        record.setTestcase("TC_01_GetCustomer");
        assertEquals(record.getTestcase(), "TC_01_GetCustomer");
    }

    @Test
    public void testSetAndGetScope() {
        Record record = new Record();
        record.setScope("Project");
        assertEquals(record.getScope(), "Project");
    }

    @Test
    public void testSetAndGetIteration() {
        Record record = new Record();
        record.setIteration("1");
        assertEquals(record.getIteration(), "1");
    }

    @Test
    public void testSetAndGetSubIteration() {
        Record record = new Record();
        record.setSubIteration("1");
        assertEquals(record.getSubIteration(), "1");
    }

    @Test
    public void testInvalidIteration_ResetToOne() {
        Record record = new Record();
        record.setIteration("invalid");
        assertEquals(record.getIteration(), "1");
    }

    @Test
    public void testInvalidSubIteration_ResetToOne() {
        Record record = new Record();
        record.setSubIteration("invalid");
        assertEquals(record.getSubIteration(), "1");
    }

    @Test
    public void testValidIterationValues() {
        Record record = new Record();

        record.setIteration("1");
        assertEquals(record.getIteration(), "1");

        record.setIteration("10");
        assertEquals(record.getIteration(), "10");

        record.setIteration("999");
        assertEquals(record.getIteration(), "999");
    }

    @Test
    public void testEmptyIterationIsValid() {
        Record record = new Record();
        record.setIteration("5");
        record.setIteration("");
        assertEquals(record.getIteration(), "");
    }

    @Test
    public void testFullRecordCreation() {
        Record record = new Record();
        record.setScenario("Customer APIs");
        record.setTestcase("TC_01_GetCustomer");
        record.setScope("Project");
        record.setIteration("1");
        record.setSubIteration("1");

        assertEquals(record.getScenario(), "Customer APIs");
        assertEquals(record.getTestcase(), "TC_01_GetCustomer");
        assertEquals(record.getScope(), "Project");
        assertEquals(record.getIteration(), "1");
        assertEquals(record.getSubIteration(), "1");
    }

    @Test
    public void testHeadersStructure() {
        assertEquals(Record.HEADERS.length, 5);
        assertEquals(Record.HEADERS[0], "Scenario");
        assertEquals(Record.HEADERS[1], "Flow");
        assertEquals(Record.HEADERS[2], "Scope");
        assertEquals(Record.HEADERS[3], "Iteration");
        assertEquals(Record.HEADERS[4], "SubIteration");
    }
}
