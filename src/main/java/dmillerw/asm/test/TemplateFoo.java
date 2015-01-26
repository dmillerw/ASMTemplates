package dmillerw.asm.test;

import dmillerw.asm.annotation.MImplement;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;

public class TemplateFoo extends Template<ClassFoo> implements Echo {

    @MOverride
    public void foo() {
        System.out.println("Override.foo()");
    }

    @MOverride
    public void bar() {
        System.out.println("Override.bar()");
    }

    @MImplement
    @Override
    public void echo() {
        _super.foo();
        _super.bar();
        foo();
        bar();
    }
}
