package dmillerw.asm;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "ASM")
public class MainMod {

    public static void main(String[] args) throws Exception {
        (new MainMod()).preInit(null);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }
}
