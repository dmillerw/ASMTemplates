package dmillerw.asm.test;

import dmillerw.asm.core.SubclassGenerator;

public class Main {

    public static void main(String[] args) throws Exception {
        ClassFoo foo2 = SubclassGenerator.generateSubclass(ClassFoo.class, TemplateFoo.class).newInstance();
        ((Echo) foo2).echo();
    }
}
