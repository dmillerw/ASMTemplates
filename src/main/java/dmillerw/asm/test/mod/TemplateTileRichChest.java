package dmillerw.asm.test.mod;

import dmillerw.asm.annotation.MField;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class TemplateTileRichChest extends Template<IInventory> {

    @MField
    public ItemStack diamond = new ItemStack(Items.diamond, 64, 0);

    @MOverride
    public ItemStack getStackInSlot(int slot) {
        return diamond;
    }
}
