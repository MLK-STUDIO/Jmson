package net.mlk.jmson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface SubJson {
    String key() default "";
    boolean checkKeyExist() default true;
    boolean includeParent() default true;
}
