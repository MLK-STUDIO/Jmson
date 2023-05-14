package net.mlk.jmson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface JsonValue {
    String key();
    Class<?> type() default JsonValue.class;
    Class<?>[] types() default JsonValue.class;
    String dateFormat() default "";
    boolean autoConvert() default true;
}
