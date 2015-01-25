package dmillerw.asm.test;

import dmillerw.asm.annotation.MConstructor;
import dmillerw.asm.annotation.MField;
import dmillerw.asm.annotation.MImplement;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;

public class TemplateFoo extends Template<ClassFoo> implements Echo {

    @MField
    public String echo;

    @MConstructor
    public void construct() {
        this.echo = "I'm a field!";
        System.out.println("TemplateFoo");
        System.out.println(echo);
    }

    @MOverride
    public void foo() {
        _super.foo();
        System.out.println("override foo");
        System.out.println(echo);
    }

    @MOverride
    public void bar() {
        _super.bar();
        System.out.println("override bar");
        System.out.println(echo);
    }

    @MImplement
    @Override
    public void echo() {
        System.out.println("TemplateFoo.echo()");
        System.out.println(echo);
    }
}
