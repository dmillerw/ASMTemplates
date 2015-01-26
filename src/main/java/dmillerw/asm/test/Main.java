package dmillerw.asm.test;

import dmillerw.asm.core.SubclassGenerator;

public class Main {

    public static void main(String[] args) throws Exception {
        SubclassGenerator<ClassFoo> generator = new SubclassGenerator<ClassFoo>(ClassFoo.class, TemplateFoo.class);
        ClassFoo foo2 = generator.generateSubclass().newInstance();
        ((Echo) foo2).echo();
    }
}
