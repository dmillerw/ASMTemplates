package dmillerw.asm.test.mod;

import dmillerw.asm.annotation.MConstructor;
import dmillerw.asm.annotation.MField;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class TemplateTileRichChest extends Template<IInventory> {

    @MField
    public ItemStack diamond;

    @MConstructor
    public void construct() {
        diamond = new ItemStack(Items.diamond, 0, 64);
    }

    @MOverride
    public ItemStack getStackInSlot(int slot) {
        return diamond.copy();
    }
}
