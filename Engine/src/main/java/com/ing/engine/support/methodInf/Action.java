
package com.ing.engine.support.methodInf;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {

    InputType input() default InputType.NO;

    ObjectType object() default ObjectType.ANY;

    InputType secondObject() default InputType.NO;

    InputType condition() default InputType.NO;

    String desc() default "";
}
