package dmillerw.asm.test;

import dmillerw.asm.core.Generator;

public class MainMod {

    public static void main(String[] args) throws Exception {
        ClassFizz fizz = Generator.generateSubclass(ClassFizz.class, TemplateFizz.class).newInstance();
        fizz.b();
    }
}
