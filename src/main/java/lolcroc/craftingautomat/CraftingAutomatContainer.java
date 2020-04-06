package lolcroc.craftingautomat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntReferenceHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CraftingAutomatContainer extends Container {
	
    public final CraftingAutomatTileEntity tile;
    protected final IntReferenceHolder ticksHolder;
    protected final IntReferenceHolder craftingFlagHolder;
    
    // Only on client
    public CraftingAutomatContainer(int id, PlayerInventory playerInventory, PacketBuffer extraData) {
    	this(id, playerInventory, (CraftingAutomatTileEntity) Minecraft.getInstance().world.getTileEntity(extraData.readBlockPos()));
    }
    
    public CraftingAutomatContainer(int id, PlayerInventory playerInventory, CraftingAutomatTileEntity te) {
    	super(CraftingAutomat.ContainerTypes.autocrafter, id);
    	this.tile = te;
    	
    	// Result slot
        te.resultHandler.ifPresent(h -> {
            addSlot(new CraftingAutomatResultSlot(h, playerInventory.player, te, 0, 124, 35));
        });
    	
    	// Crafting matrix
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                int finalI = i;
                int finalJ = j;
                te.matrixHandler.ifPresent(h -> {
                    addSlot(new SlotItemHandlerUpdatesRecipe(te, h, finalJ + finalI * 3, 30 + finalJ * 18, 17 + finalI * 18));
                });
            }
        }
        
        // Crafting buffer
        for (int l = 0; l < 9; ++l) {
            int finalL = l;
            te.bufferHandler.ifPresent(h -> {
                addSlot(new SlotItemHandlerUpdatesHelper(te, h, finalL, 8 + finalL * 18, 84));
            });
        }

        // Player inventory
        for (int k = 0; k < 3; ++k) {
            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(playerInventory, i1 + k * 9 + 9, 8 + i1 * 18, 115 + k * 18));
            }
        }

        // Player hotbar
        for (int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(playerInventory, l, 8 + l * 18, 173));
        }

        // Adds trackers for ticksActive and craftingFlag
        ticksHolder = te.ticksHolder;
        craftingFlagHolder = te.craftingFlagHolder;
        trackInt(ticksHolder);
        trackInt(craftingFlagHolder);
    }
    
    @OnlyIn(Dist.CLIENT)
    public int getProgress() {
        return ticksHolder.get();
    }

    @OnlyIn(Dist.CLIENT)
    public CraftingAutomatTileEntity.CraftingFlag getCraftingFlag() {
        return CraftingAutomatTileEntity.CraftingFlag.fromIndex(craftingFlagHolder.get());
    }

	@Override
	public boolean canInteractWith(PlayerEntity player) {
        return isWithinUsableDistance(IWorldPosCallable.of(tile.getWorld(), tile.getPos()), player, CraftingAutomat.Blocks.autocrafter);
	}
	
	@Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return !(slot instanceof CraftingAutomatResultSlot) && super.canMergeSlot(stack, slot);
    }
	
	@Override
	public ItemStack transferStackInSlot(PlayerEntity playerIn, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index == 0) {
                itemstack1.getItem().onCreated(itemstack1, playerIn.world, playerIn);

                // Merge result slot to player inv
                if (!this.mergeItemStack(itemstack1, 19, 55, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onSlotChange(itemstack1, itemstack);
            }
            else if (index >= 1 && index < 10) {
            	// Merge matrix to buffer, then to full player inv
            	if (!this.mergeItemStack(itemstack1, 10, 19, false) && !this.mergeItemStack(itemstack1, 19, 55, true)) {
            		return ItemStack.EMPTY;
            	}
            }
            else if (index >= 10 && index < 19) {
            	// Merge buffer to full player inv
            	if (!this.mergeItemStack(itemstack1, 19, 55, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 19 && index < 46) {
            	// Merge player inv to buffer, then to hotbar
            	if (!this.mergeItemStack(itemstack1, 10, 19, false) && !this.mergeItemStack(itemstack1, 46, 55, false)) {
            		return ItemStack.EMPTY;
            	}
            }
            else if (!this.mergeItemStack(itemstack1, 10, 19, false) && !this.mergeItemStack(itemstack1, 19, 46, false)) {
            	// Merge hotbar to buffer, then to player inv
            	return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            }
            else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            ItemStack itemstack2 = slot.onTake(playerIn, itemstack1);

            if (index == 0) {
                playerIn.dropItem(itemstack2, false);
            }
        }

        return itemstack;
    }

    private class SlotItemHandlerUpdatesRecipe extends SlotItemHandler {
        private final CraftingAutomatTileEntity tile;

        public SlotItemHandlerUpdatesRecipe(CraftingAutomatTileEntity te, IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            tile = te;
        }

        @Override
        public void onSlotChanged() {
            tile.updateRecipe();
            tile.markDirty();
            super.onSlotChanged();
        }
    }

    private class SlotItemHandlerUpdatesHelper extends SlotItemHandler {
        private final CraftingAutomatTileEntity tile;

        public SlotItemHandlerUpdatesHelper(CraftingAutomatTileEntity te, IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            tile = te;
        }

        @Override
        public void onSlotChanged() {
            tile.updateHelper();
            tile.markDirty();
            super.onSlotChanged();
        }
    }

}
