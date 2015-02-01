package dmillerw.asm.test;

import dmillerw.asm.annotation.MCastParam;
import dmillerw.asm.annotation.MCastParamList;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.core.Template;

public class TemplateFoo extends Template<ClassFoo> {

    @MOverride
    @MCastParamList( castParams = {
            @MCastParam(index = -1, cast = "dmillerw.asm.test.ClassBar"),
            @MCastParam(index = 0, cast = "dmillerw.asm.test.ClassBar")
            } )
    public Object foo(Object bar) {
        ((Echo)bar).echo();
        return bar;
    }

    @MOverride
    public void echo() {
        System.out.println("ECHOOO!");
    }
}
