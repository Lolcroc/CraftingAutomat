package lolcroc.craftingautomat.inventory;

import java.lang.reflect.Field;

import lolcroc.craftingautomat.tileentity.TileEntityCraftingAutomat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerCraftingAutomat extends Container {
	
    public final TileEntityCraftingAutomat inventory;
    private final EntityPlayer player;
    
    private int progress;
    private int maxProgress;
    
    public ContainerCraftingAutomat(InventoryPlayer playerInventory, TileEntityCraftingAutomat crafterInventory) {
    	this.inventory = crafterInventory;
    	this.player = playerInventory.player;
    	crafterInventory.openInventory(player);
    	
    	// Result slot
    	this.addSlotToContainer(new SlotAutoCrafting(playerInventory.player, crafterInventory, 0, 124, 35));
    	
    	// Crafting matrix
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlotToContainer(new Slot(crafterInventory, 1 + j + i * 3, 30 + j * 18, 17 + i * 18));
            }
        }
        
        // Crafting buffer
        for (int l = 0; l < 9; ++l) {
            this.addSlotToContainer(new Slot(crafterInventory, 10 + l, 8 + l * 18, 84));
        }

        // Player inventory
        for (int k = 0; k < 3; ++k) {
            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlotToContainer(new Slot(playerInventory, i1 + k * 9 + 9, 8 + i1 * 18, 115 + k * 18));
            }
        }

        // Player hotbar
        for (int l = 0; l < 9; ++l) {
            this.addSlotToContainer(new Slot(playerInventory, l, 8 + l * 18, 173));
        }
    }
    
    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, this.inventory);
    }
    
    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        for (int i = 0; i < this.listeners.size(); ++i)
        {
            IContainerListener icontainerlistener = this.listeners.get(i);

            if (this.progress != this.inventory.getField(0))
            {
                icontainerlistener.sendWindowProperty(this, 0, this.inventory.getField(0));
            }

            if (this.maxProgress != this.inventory.getField(1))
            {
                icontainerlistener.sendWindowProperty(this, 1, this.inventory.getField(1));
            }
        }

        this.progress = this.inventory.getField(0);
        this.maxProgress = this.inventory.getField(1);
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int id, int data) {
        this.inventory.setField(id, data);
    }

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return this.inventory.isUsableByPlayer(player);
	}
	
	@Override
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        return slotIn.inventory != this.inventory.craftResult && super.canMergeSlot(stack, slotIn);
    }
	
	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		this.inventory.closeInventory(playerIn);
	}
	
	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index)
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
                //System.out.println("test");
            }
            else if (index >= 10 && index < 19) {
            	// Merge crafting buffer to player inv
            	if (!this.mergeItemStack(itemstack1, 19, 55, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.mergeItemStack(itemstack1, 10, 19, false)) {
            	// Merge the crafting matrix [1-10> AND player inv [19-55> to the buffer [10-19>
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            }
            else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }

            ItemStack itemstack2 = slot.onTake(playerIn, itemstack1);

            if (index == 0)
            {
            	//TODO dispense the item ???
                playerIn.dropItem(itemstack2, false);
            }
        }

        return itemstack;
    }

}
