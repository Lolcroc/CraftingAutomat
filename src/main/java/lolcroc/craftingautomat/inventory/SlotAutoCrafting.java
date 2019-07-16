package lolcroc.craftingautomat.inventory;

import com.google.common.collect.Lists;

import lolcroc.craftingautomat.tileentity.TileEntityCraftingAutomat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public class SlotAutoCrafting extends SlotCrafting {

	private final TileEntityCraftingAutomat inventory;
	private final EntityPlayer player;
	
	private int amountCrafted;
	
	public SlotAutoCrafting(EntityPlayer player, TileEntityCraftingAutomat te, int slotIndex, int xPosition, int yPosition) {
		super(player, te.craftMatrix, te, slotIndex, xPosition, yPosition);
		this.inventory = te;
		this.player = player;
	}
	
	@Override
    public ItemStack decrStackSize(int amount) {
        if (this.getHasStack()) {
            this.amountCrafted += Math.min(amount, this.getStack().getCount());
        }

        return super.decrStackSize(amount);
    }

    @Override
    protected void onCrafting(ItemStack stack, int amount) {
        this.amountCrafted += amount;
        this.onCrafting(stack);
    }

    @Override
    protected void onSwapCraft(int p_190900_1_) {
        this.amountCrafted += p_190900_1_;
    }
	
	@Override
	protected void onCrafting(ItemStack stack) {
		//Fire Item.onCrafting and Forge crafting hook
        if (this.amountCrafted > 0)
        {
            stack.onCrafting(this.player.world, this.player, this.amountCrafted);
            net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerCraftingEvent(this.player, stack, this.inventory.craftMatrix);
        }

        this.amountCrafted = 0;
        
        //Unlock recipe
        InventoryCraftResult inventorycraftresult = (InventoryCraftResult)this.inventory.craftResult;
        IRecipe irecipe = inventorycraftresult.getRecipeUsed();

        if (irecipe != null && !irecipe.isDynamic())
        {
            this.player.unlockRecipes(Lists.newArrayList(irecipe));
            //inventorycraftresult.setRecipeUsed((IRecipe)null);
        }
    }
	
	@Override
	public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
		this.onCrafting(stack);
        net.minecraftforge.common.ForgeHooks.setCraftingPlayer(thePlayer);
        this.inventory.tryCraft(thePlayer);
        this.inventory.markDirty();
        net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
        return stack;
	}

}
