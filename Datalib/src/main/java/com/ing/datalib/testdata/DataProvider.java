
package com.ing.datalib.testdata;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * 
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface DataProvider {

    String type() default "csv";

}
