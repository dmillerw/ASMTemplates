package dmillerw.asm.test;

import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class TemplateTileRichChest extends Template<IInventory> {

    @MOverride
    public ItemStack getStackInSlot(int slot) {
        return new ItemStack(Items.diamond, 64, 0);
    }
}
