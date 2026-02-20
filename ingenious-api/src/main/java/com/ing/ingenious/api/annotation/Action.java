package com.ing.ingenious.api.annotation;

import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {

    InputType input() default InputType.NO;

    String object() default ObjectType.ANY;

    InputType secondObject() default InputType.NO;

    InputType condition() default InputType.NO;

    String desc() default "";
}
