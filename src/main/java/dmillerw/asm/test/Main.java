package dmillerw.asm.test;

import dmillerw.asm.core.SubclassGenerator;

public class Main {

    public static void main(String[] args) throws Exception {
        ClassFoo classFoo = new ClassFoo();
        ClassBar classBar = new ClassBar();

        classFoo.foo(classBar);

        ClassFoo foo2 = SubclassGenerator.generateSubclass(ClassFoo.class, TemplateFoo.class).newInstance();
        foo2.foo(classBar);
    }
}
