package dmillerw.asm.test;

public class ClassFoo extends ClassBar {

    public ClassBar foo(ClassBar bar) {
        System.out.println("ClassFoo: " + bar.toString());
        return bar;
    }
}
