package dmillerw.asm.test;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dmillerw.asm.core.Generator;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "ASM")
public class MainMod {

    public static void main(String[] args) throws Exception {
        ClassFoo foo1 = new ClassFoo();
        foo1.a();
        foo1.b();

        System.out.println(foo1 instanceof Echo);
        if (foo1 instanceof Echo) {
            ((Echo) foo1).echo();
        }

        ClassFoo foo2 = Generator.generateSubclass(ClassFoo.class, new TemplateFoo()).newInstance();
        foo2.a();
        foo2.b();

        System.out.println(foo2 instanceof Echo);
        if (foo2 instanceof Echo) {
            ((Echo) foo2).echo();
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }
}
