package lolcroc.craftingautomat;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.hooks.BasicEventHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CraftingAutomatResultSlot extends SlotItemHandler {

	private final CraftingAutomatTileEntity tile;
	private final PlayerEntity player;
	private int amountCrafted;
	
	public CraftingAutomatResultSlot(IItemHandler handler, PlayerEntity player, CraftingAutomatTileEntity te, int slotIndex, int xPosition, int yPosition) {
		super(handler, slotIndex, xPosition, yPosition);
		this.tile = te;
		this.player = player;
	}

	@Override
    public boolean isItemValid(ItemStack stack) {
	    return false;
    }
	
	@Override
    public ItemStack decrStackSize(int amount) {
        if (getHasStack()) {
            amountCrafted += Math.min(amount, getStack().getCount());
        }

        return super.decrStackSize(amount);
    }

    @Override
    protected void onCrafting(ItemStack stack, int amount) {
        amountCrafted += amount;
        onCrafting(stack);
    }

    @Override
    protected void onSwapCraft(int p_190900_1_) {
	    amountCrafted += p_190900_1_;
    }

    @Override
    public void onSlotChange(ItemStack stack, ItemStack other) {
        int i = other.getCount() - stack.getCount();
        if (i > 0) {
            onCrafting(other, i);
        }
    }
	
	@Override
	protected void onCrafting(ItemStack stack) {
		//Fire Item.onCrafting and Forge crafting hook
        if (amountCrafted > 0) {
            stack.onCrafting(player.world, player, amountCrafted);
            tile.matrixWrapper.ifPresent(h -> BasicEventHooks.firePlayerCraftingEvent(player, stack, h));
        }
        amountCrafted = 0;
        
        //Unlock recipe
        IRecipe recipe = tile.getRecipeUsed();
        if (recipe != null && !recipe.isDynamic()) {
            player.unlockRecipes(Lists.newArrayList(recipe));
        }
    }
	
	@Override
	public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
		onCrafting(stack);
        ForgeHooks.setCraftingPlayer(thePlayer);
        tile.consumeIngredients(thePlayer);
        tile.updateRecipe();
        ForgeHooks.setCraftingPlayer(null);
        return stack;
	}

}
