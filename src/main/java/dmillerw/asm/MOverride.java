package dmillerw.asm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MOverride {

    /**
     * Whether the contents of this method should directly be copied to the subclass
     * Setting this to false (or not supplying it) will have this method be called via the callback instance
     *
     * @return
     */
    boolean copy() default false;
}
