package dmillerw.asm.test;

import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;

public class TemplateFizz extends Template<ClassFizz> {

    @MOverride()
    public void b() {
        _super.b();
        System.out.println("TemplateFizz.b() calling super.b()");
    }
}
