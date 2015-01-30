package dmillerw.asm.test;

public class ClassBar implements Echo {

    @Override
    public String toString() {
        return "ClassBar";
    }

    @Override
    public void echo() {
        System.out.println(this.getClass().getName());
    }
}
