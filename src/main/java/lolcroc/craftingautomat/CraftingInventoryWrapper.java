package lolcroc.craftingautomat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

public class CraftingInventoryWrapper extends CraftingInventory {

    private final IItemHandlerModifiable inv;

    public CraftingInventoryWrapper(IItemHandlerModifiable handler) {
        super(null, 3, 3);
        this.inv = handler;
    }

    @Override
    public int getSizeInventory() {
        return inv.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for(int i = 0; i < inv.getSlots(); i++) {
            if (!inv.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inv.getStackInSlot(index);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack s = getStackInSlot(index);
        if(s.isEmpty()) return ItemStack.EMPTY;
        setInventorySlotContents(index, ItemStack.EMPTY);
        return s;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = inv.getStackInSlot(index);
        return stack.isEmpty() ? ItemStack.EMPTY : stack.split(count);
    }

    @Override
    public void setInventorySlotContents(int index, @Nonnull ItemStack stack) {
        inv.setStackInSlot(index, stack);
    }

    @Override
    public void clear() {
        for(int i = 0; i < inv.getSlots(); i++) {
            inv.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
