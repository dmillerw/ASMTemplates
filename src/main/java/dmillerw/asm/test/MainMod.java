package dmillerw.asm.test;

import dmillerw.asm.core.Generator;

public class MainMod {

    public static void main(String[] args) throws Exception {
        ClassFoo foo1 = new ClassFoo();
        foo1.a();
        foo1.b();

        System.out.println(foo1 instanceof Echo);
        if (foo1 instanceof Echo) {
            ((Echo) foo1).echo();
        }

        ClassBar foo2 = Generator.generateSubclass(ClassBar.class, TemplateBar.class).newInstance();
        foo2.a();
        foo2.b();

        System.out.println(foo2 instanceof Echo);
        if (foo2 instanceof Echo) {
            ((Echo) foo2).echo();
        }
    }
}
