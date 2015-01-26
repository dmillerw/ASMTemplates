package dmillerw.asm.test.mod;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import dmillerw.asm.core.SubclassGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;

@Mod(modid = "ASM")
public class MainMod {

    public static Block richChest;

    public static Class<? extends TileEntity> tileRichChest;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        richChest = SubclassGenerator.generateSubclass(BlockChest.class, TemplateRichChest.class).getConstructor(int.class).newInstance(0);
        tileRichChest = SubclassGenerator.generateSubclass(TileEntityChest.class, TemplateTileRichChest.class);

        GameRegistry.registerBlock(richChest, "richChest");
        GameRegistry.registerTileEntity(tileRichChest, "tileRichChest");
    }
}
