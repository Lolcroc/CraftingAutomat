package lolcroc.craftingautomat;

import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;

public class BufferHandler extends UnsafeItemStackHandler implements StackedContentsCompatible {

    private final CraftingAutomatBlockEntity tile;

    public BufferHandler(CraftingAutomatBlockEntity te) {
        super(9);
        this.tile = te;
    }

    @Override
    protected void onContentsChanged(int slot) {
        tile.updateHelper();
        tile.setChanged();
    }

    @Override
    protected void onLoad() {
        tile.updateHelper();
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        contents.clear();
        for(ItemStack itemstack : stacks) {
            contents.accountSimpleStack(itemstack);
        }
    }
}
