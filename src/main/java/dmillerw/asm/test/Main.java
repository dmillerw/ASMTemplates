package dmillerw.asm.test;

import dmillerw.asm.core.Generator;

public class Main {

    public static void main(String[] args) throws Exception {
        ClassFoo foo1 = new ClassFoo();
        foo1.foo();
        foo1.bar();

        System.out.println(foo1 instanceof Echo);
        if (foo1 instanceof Echo) {
            ((Echo) foo1).echo();
        }

        ClassFoo foo2 = Generator.generateSubclass(ClassFoo.class, TemplateFoo.class).newInstance();
        foo2.foo();
        foo2.bar();

        System.out.println(foo2 instanceof Echo);
        if (foo2 instanceof Echo) {
            ((Echo) foo2).echo();
        }
    }
}
