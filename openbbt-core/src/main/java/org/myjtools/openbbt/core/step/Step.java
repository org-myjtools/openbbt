package org.myjtools.openbbt.core.step;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Step {

    String value();

    String[] args() default {};

}