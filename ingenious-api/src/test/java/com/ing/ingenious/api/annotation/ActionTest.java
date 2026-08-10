package com.ing.ingenious.api.annotation;

import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ActionTest {

    @Action
    private static class DefaultActionHolder {
    }

    @Action(
            input = InputType.YES,
            object = ObjectType.WEB,
            secondObject = InputType.OPTIONAL,
            condition = InputType.NO,
            desc = "Click action"
    )
    private static class CustomActionHolder {
    }

    @Test
    void annotationDefaultsShouldMatchContract() {
        Action action = DefaultActionHolder.class.getAnnotation(Action.class);

        assertNotNull(action);
        assertEquals(InputType.NO, action.input());
        assertEquals(ObjectType.ANY, action.object());
        assertEquals(InputType.NO, action.secondObject());
        assertEquals(InputType.NO, action.condition());
        assertEquals("", action.desc());
    }

    @Test
    void annotationCustomValuesShouldBeReadableAtRuntime() {
        Action action = CustomActionHolder.class.getAnnotation(Action.class);

        assertNotNull(action);
        assertEquals(InputType.YES, action.input());
        assertEquals(ObjectType.WEB, action.object());
        assertEquals(InputType.OPTIONAL, action.secondObject());
        assertEquals(InputType.NO, action.condition());
        assertEquals("Click action", action.desc());
    }

    @Test
    void annotationShouldBeRuntimeRetained() throws NoSuchMethodException {
        Method method = Action.class.getMethod("desc");
        assertNotNull(method);
    }
}
