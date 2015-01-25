package dmillerw.asm.test;

import dmillerw.asm.annotation.MConstructor;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;

public class TemplateFoo extends Template<ClassFoo> {

    @MConstructor
    public void construct() {
        System.out.println("Constructed SubFoo");
    }

    @MOverride()
    public void a() {
        _super.b();
    }
}
