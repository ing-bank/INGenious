package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class ParameterTest {

    // ---- Constructor tests ----

    @Test
    public void testDefaultConstructor() {
        Parameter p = new Parameter();
        assertThat(p.getIteration()).isEqualTo(1);
        assertThat(p.getSubIteration()).isEqualTo(1);
    }

    @Test
    public void testParameterizedConstructor() {
        Parameter p = new Parameter(3, 5);
        assertThat(p.getIteration()).isEqualTo(3);
        assertThat(p.getSubIteration()).isEqualTo(5);
    }

    // ---- Fluent API tests ----

    @Test
    public void testWithIteration() {
        Parameter p = new Parameter();
        Parameter result = p.withIteration(10);
        assertThat(result).isSameAs(p);
        assertThat(p.getIteration()).isEqualTo(10);
    }

    @Test
    public void testWithSubIteration() {
        Parameter p = new Parameter();
        Parameter result = p.withSubIteration(7);
        assertThat(result).isSameAs(p);
        assertThat(p.getSubIteration()).isEqualTo(7);
    }

    // ---- resolveMaxIter tests ----

    @Test
    public void testResolveMaxIterNull() {
        assertThat(Parameter.resolveMaxIter(null)).isEqualTo(1);
    }

    @Test
    public void testResolveMaxIterEmpty() {
        assertThat(Parameter.resolveMaxIter("")).isEqualTo(1);
    }

    @Test
    public void testResolveMaxIterSingle() {
        assertThat(Parameter.resolveMaxIter("Single")).isEqualTo(1);
        assertThat(Parameter.resolveMaxIter("single")).isEqualTo(1);
    }

    @Test
    public void testResolveMaxIterAll() {
        assertThat(Parameter.resolveMaxIter("All")).isEqualTo(-1);
        assertThat(Parameter.resolveMaxIter("all")).isEqualTo(-1);
    }

    @Test
    public void testResolveMaxIterNumeric() {
        assertThat(Parameter.resolveMaxIter("5")).isEqualTo(5);
        assertThat(Parameter.resolveMaxIter("1")).isEqualTo(1);
    }

    @Test
    public void testResolveMaxIterRange() {
        assertThat(Parameter.resolveMaxIter("2:8")).isEqualTo(8);
        assertThat(Parameter.resolveMaxIter("1:3")).isEqualTo(3);
    }

    @Test
    public void testResolveMaxIterUnknown() {
        assertThat(Parameter.resolveMaxIter("abc")).isEqualTo(-1);
    }

    // ---- resolveStartIter tests ----

    @Test
    public void testResolveStartIterNull() {
        assertThat(Parameter.resolveStartIter(null)).isEqualTo(1);
    }

    @Test
    public void testResolveStartIterEmpty() {
        assertThat(Parameter.resolveStartIter("")).isEqualTo(1);
    }

    @Test
    public void testResolveStartIterSingle() {
        assertThat(Parameter.resolveStartIter("Single")).isEqualTo(1);
    }

    @Test
    public void testResolveStartIterAll() {
        assertThat(Parameter.resolveStartIter("All")).isEqualTo(1);
    }

    @Test
    public void testResolveStartIterNumeric() {
        assertThat(Parameter.resolveStartIter("5")).isEqualTo(5);
    }

    @Test
    public void testResolveStartIterRange() {
        assertThat(Parameter.resolveStartIter("3:8")).isEqualTo(3);
        assertThat(Parameter.resolveStartIter("1:5")).isEqualTo(1);
    }

    @Test
    public void testResolveStartIterUnknown() {
        assertThat(Parameter.resolveStartIter("abc")).isEqualTo(1);
    }

    // ---- Condition matchers ----

    @Test
    public void testStartParamRLoop() {
        assertThat(Parameter.startParamRLoop("Start Param")).isTrue();
        assertThat(Parameter.startParamRLoop("Start Loop:3")).isTrue();
        assertThat(Parameter.startParamRLoop("End Param")).isFalse();
        assertThat(Parameter.startParamRLoop("Random")).isFalse();
    }

    @Test
    public void testIsLoop() {
        assertThat(Parameter.isLoop("Start Loop:3")).isTrue();
        assertThat(Parameter.isLoop("End Loop")).isTrue();
        assertThat(Parameter.isLoop("Start Param")).isFalse();
    }

    @Test
    public void testEndParamRLoop() {
        assertThat(Parameter.endParamRLoop("End Param")).isTrue();
        assertThat(Parameter.endParamRLoop("End Loop")).isTrue();
        assertThat(Parameter.endParamRLoop("Start Param")).isFalse();
    }

    // ---- toString ----

    @Test
    public void testToString() {
        Parameter p = new Parameter(2, 3);
        assertThat(p.toString()).isEqualTo("2:3");
    }
}
