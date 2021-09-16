package lolcroc.craftingautomat;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

public class ReversedInvWrapper implements IItemHandlerModifiable {
    
    protected final IItemHandlerModifiable originalHandler;
    
    public ReversedInvWrapper(IItemHandlerModifiable orig) {
        originalHandler = orig;
    }

    private int getReversedSlot(int slot) {
        return getSlots() - 1 - slot;
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        originalHandler.setStackInSlot(getReversedSlot(slot), stack);
    }

    @Override
    public int getSlots() {
        return originalHandler.getSlots();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return originalHandler.getStackInSlot(getReversedSlot(slot));
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return originalHandler.insertItem(getReversedSlot(slot), stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return originalHandler.extractItem(getReversedSlot(slot), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return originalHandler.getSlotLimit(getReversedSlot(slot));
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return originalHandler.isItemValid(getReversedSlot(slot), stack);
    }
}
