package lolcroc.craftingautomat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CraftingAutomatContainer extends AbstractContainerMenu {

    private final CraftingAutomatBlockEntity tile;
    private final DataSlot ticksHolder;
    private final DataSlot craftingFlagHolder;
    
    // Only on client
    public CraftingAutomatContainer(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, (CraftingAutomatBlockEntity) Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos()));
    }
    
    public CraftingAutomatContainer(int id, Inventory playerInventory, CraftingAutomatBlockEntity te) {
        super(CraftingAutomat.AUTOCRAFTER_MENU.get(), id);
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
        addDataSlot(ticksHolder);
        addDataSlot(craftingFlagHolder);
    }
    
    @OnlyIn(Dist.CLIENT)
    public int getProgress() {
        return ticksHolder.get();
    }

    @OnlyIn(Dist.CLIENT)
    public CraftingAutomatBlockEntity.CraftingFlag getCraftingFlag() {
        return CraftingAutomatBlockEntity.CraftingFlag.fromIndex(craftingFlagHolder.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()), player, CraftingAutomat.AUTOCRAFTER_BLOCK.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof CraftingAutomatResultSlot) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 0) {
                itemstack1.getItem().onCraftedBy(itemstack1, player.level, player);

                // Merge result slot to player inv
                if (!moveItemStackTo(itemstack1, 19, 55, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }
            else if (index >= 1 && index < 10) {
                // Merge matrix to buffer, then to full player inv
                if (!moveItemStackTo(itemstack1, 10, 19, false) && !moveItemStackTo(itemstack1, 19, 55, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 10 && index < 19) {
                // Merge buffer to full player inv
                if (!moveItemStackTo(itemstack1, 19, 55, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 19 && index < 46) {
                // Merge player inv to buffer, then to hotbar
                if (!moveItemStackTo(itemstack1, 10, 19, false) && !moveItemStackTo(itemstack1, 46, 55, false)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (!moveItemStackTo(itemstack1, 10, 19, false) && !moveItemStackTo(itemstack1, 19, 46, false)) {
                // Merge hotbar to buffer, then to player inv
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
            else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);

            if (index == 0) {
                player.drop(itemstack1, false);
            }
        }

        return itemstack;
    }

    private static class SlotItemHandlerUpdatesRecipe extends SlotItemHandler {
        private final CraftingAutomatBlockEntity tile;

        public SlotItemHandlerUpdatesRecipe(CraftingAutomatBlockEntity te, IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            tile = te;
        }

        @Override
        public void setChanged() {
            tile.updateRecipe();
            tile.setChanged();
            super.setChanged();
        }
    }

    private static class SlotItemHandlerUpdatesHelper extends SlotItemHandler {
        private final CraftingAutomatBlockEntity tile;

        public SlotItemHandlerUpdatesHelper(CraftingAutomatBlockEntity te, IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            tile = te;
        }

        @Override
        public void setChanged() {
            tile.updateHelper();
            tile.setChanged();
            super.setChanged();
        }
    }

}
