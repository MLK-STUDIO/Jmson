package net.mlk.jmson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsonObject {
    String key() default "";
    String[] keyList() default "";
    boolean ignoreNull() default false;
    boolean checkExist() default true;
}
