package lolcroc.craftingautomat.inventory;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.util.NonNullList;

public class InventoryAutoCrafting extends InventoryCrafting {

	/*
	 * Derpy dummy class without the Container callbacks
	 * 
	 */
	private final NonNullList<ItemStack> stacks;

	public InventoryAutoCrafting() {
		super(null, 3, 3);
		this.stacks = NonNullList.<ItemStack>withSize(this.getHeight() * this.getWidth(), ItemStack.EMPTY);
	}

	@Override
	public int getSizeInventory() {
		return this.stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		return index >= this.getSizeInventory() ? ItemStack.EMPTY : (ItemStack)this.stacks.get(index);
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		return ItemStackHelper.getAndRemove(this.stacks, index);
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		return ItemStackHelper.getAndSplit(this.stacks, index, count);
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		this.stacks.set(index, stack);
	}

	@Override
	public void clear() {
		this.stacks.clear();
	}

	@Override
	public void fillStackedContents(RecipeItemHelper helper) {
		for (ItemStack itemstack : this.stacks) {
			helper.accountPlainStack(itemstack);
		}
	}

}
