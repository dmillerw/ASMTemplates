package dmillerw.asm;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class EventHandler {

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) throws Exception {
        ItemStack held = event.entityPlayer.getHeldItem();
        if (held != null && held.getItem() == Items.diamond) {
            TileEntity tileEntity = event.world.getTileEntity(event.x, event.y, event.z);
            if (tileEntity != null && tileEntity instanceof IInventory) {
                Class<TileEntity> replaceClass = (Class<TileEntity>) Generator.generateSubclass(
                        tileEntity.getClass(),
                        new TemplateInventory()
                );
                TileEntity tileEntity1 = replaceClass.newInstance();

                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                tileEntity.writeToNBT(nbtTagCompound);
                tileEntity.invalidate();
                event.world.setTileEntity(event.x, event.y, event.z, tileEntity1);
                tileEntity1.readFromNBT(nbtTagCompound);
            }
        }
    }
}
