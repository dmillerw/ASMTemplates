package dmillerw.asm.test;

import dmillerw.asm.annotation.MCastParam;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.core.Template;

public class TemplateFoo extends Template<ClassFoo> {

    @MOverride
    @MCastParam(index = 0, cast = "dmillerw.asm.test.ClassBar")
    public void foo(Object bar) {
        ((Echo)bar).echo();
    }
}
