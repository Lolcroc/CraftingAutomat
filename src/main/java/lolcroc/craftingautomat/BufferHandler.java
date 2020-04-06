package lolcroc.craftingautomat;

import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraftforge.items.ItemStackHandler;

public class BufferHandler extends ItemStackHandler implements IRecipeHelperPopulator {

    private final CraftingAutomatTileEntity tile;

    public BufferHandler(CraftingAutomatTileEntity te) {
        super(9);
        this.tile = te;
    }

    @Override
    protected void onContentsChanged(int slot) {
        tile.updateHelper();
        tile.markDirty();
    }

    @Override
    protected void onLoad() {
        tile.updateHelper();
    }

    @Override
    public void fillStackedContents(RecipeItemHelper helper) {
        helper.clear();
        for(ItemStack itemstack : stacks) {
            helper.accountPlainStack(itemstack);
        }
    }
}
