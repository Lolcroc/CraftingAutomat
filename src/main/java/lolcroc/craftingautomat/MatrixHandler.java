package lolcroc.craftingautomat;

public class MatrixHandler extends UnsafeItemStackHandler {

    private final CraftingAutomatBlockEntity tile;

    public MatrixHandler(CraftingAutomatBlockEntity te) {
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
