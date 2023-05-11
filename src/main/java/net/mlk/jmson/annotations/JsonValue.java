package net.mlk.jmson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface JsonValue {
    String name();
    Class<?> type() default JsonValue.class;
    boolean autoConvert() default true;
}
