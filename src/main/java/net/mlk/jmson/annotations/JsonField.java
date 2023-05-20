package net.mlk.jmson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface JsonField {
    String key() default "JmsonKeyTemplate";
    String dateFormat() default "";
    Class<?> type() default JsonField.class;
    Class<?>[] types() default {};
    boolean ignoreNull() default false;
}
