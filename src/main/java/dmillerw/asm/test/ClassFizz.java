package dmillerw.asm.test;

public class ClassFizz extends ClassBar {

    @Override
    public void b() {
        super.b();
        System.out.println("ClassFizz.b() calling super.b()");
    }
}
