package dmillerw.asm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MCastParam {

    /**
     * Index of the parameter to be cast. 0 being the first parameter, and so on
     * Remember that (%CLASS% ... param) arguments are compiled to a simple array param
     *
     * Casting the return value should be done with an index of -1
     */
    int index();

    /**
     * Class the param or return value should be cast too.
     *
     * Should be a fully qualified name, with either . or / path separators
     */
    String cast();
}
