package dmillerw.asm.test;

import dmillerw.asm.annotation.MConstructor;
import dmillerw.asm.annotation.MImplement;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;

public class TemplateFoo extends Template<ClassFoo> implements Echo {

    @MConstructor
    public void construct() {
        System.out.println("Constructed SubFoo");
    }

    @MOverride()
    public void a() {
        _super.b();
    }

    @MOverride
    public void c() {

    }

    @Override
    @MImplement
    public void echo() {
        System.out.println("ECHO");
    }
}
