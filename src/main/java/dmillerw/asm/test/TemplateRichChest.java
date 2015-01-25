package dmillerw.asm.test;

import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;
import net.minecraft.block.BlockContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TemplateRichChest extends Template<BlockContainer> {

    @MOverride
    public TileEntity createNewTileEntity(World world, int meta) {
        try {
            return MainMod.tileRichChest.newInstance();
        } catch (Exception ex) {
            return null;
        }
    }
}
