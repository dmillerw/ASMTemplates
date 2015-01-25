package dmillerw.asm;

import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class TemplateInventory extends Template<IInventory> {

    @MOverride
    public ItemStack getStackInSlot(int slot) {
        return new ItemStack(Items.diamond_axe);
    }
}
