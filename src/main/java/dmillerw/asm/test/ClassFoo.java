package dmillerw.asm.test;

public class ClassFoo {

    public ClassBar foo(ClassBar bar) {
        System.out.println("ClassFoo: " + bar.toString());
        return bar;
    }
}
