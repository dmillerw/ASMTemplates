package dmillerw.asm.test;

public class ClassBar extends ClassFoo {

    @Override
    public void b() {
        super.a();
        System.out.println("ClassBar.b() calling super.a()");
    }
}
