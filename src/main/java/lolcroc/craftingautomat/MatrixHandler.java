package lolcroc.craftingautomat;

import net.minecraftforge.items.ItemStackHandler;

public class MatrixHandler extends ItemStackHandler {

    private final CraftingAutomatTileEntity tile;

    public MatrixHandler(CraftingAutomatTileEntity te) {
        super(9);
        tile = te;
    }

    @Override
    protected void onContentsChanged(int slot) {
        tile.updateRecipe();
        tile.setChanged();
    }

    // No updateRecipe() in onLoad(): There is no world (and thus no recipemanager) during nbt deserialization :(
    // I fixed this in onLoad() of the TileEntity
}
