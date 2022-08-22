package lolcroc.craftingautomat;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class UnsafeItemStackHandler extends ItemStackHandler {

    public UnsafeItemStackHandler(int size) {
        super(size);
    }

    public void unsafeSetStackInSlot(int slot, @NotNull ItemStack stack) {
        validateSlotIndex(slot);
        this.stacks.set(slot, stack);
    }
}
