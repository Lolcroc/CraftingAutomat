package lolcroc.craftingautomat.tileentity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lolcroc.craftingautomat.CraftingAutomat;
import lolcroc.craftingautomat.block.BlockCraftingAutomat;
import lolcroc.craftingautomat.inventory.ContainerCraftingAutomat;
import lolcroc.craftingautomat.inventory.InventoryAutoCrafting;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class TileEntityCraftingAutomat extends TileEntity implements ITickable, ISidedInventory {

	/* SLOT IDs:
	 *       0:	Result
	 *  [1-10>: Matrix
	 * [10-19>:	Buffer
	 * */
	private static final int[] SLOTS_RESULT = new int[] {0};
	private static final int[] SLOTS_MATRIX = IntStream.range(1, 10).toArray();
	private static final int[] SLOTS_BUFFER = IntStream.range(10, 19).toArray();
	private static final int[] SLOTS_BUFFER_REV = IntStream.range(10, 19).map(i -> 28 - i).toArray();
	
	public static final int CRAFTING_TICKS = 8;
	public static final int COOLDOWN_TICKS = 16;

	private NonNullList<ItemStack> buffer = NonNullList.<ItemStack>withSize(9, ItemStack.EMPTY);;
	public InventoryCrafting craftMatrix = new InventoryAutoCrafting();
	public InventoryCraftResult craftResult = new InventoryCraftResult();

	private String customName;
	private RecipeHelper recipeHelper;
	private final DispenseHelper dispenseHelper = new DispenseHelper();
	
	private int ticksActive;

	public TileEntityCraftingAutomat() {
		this.recipeHelper = new RecipeHelper(this, this.craftResult, this.craftMatrix);
	}
	
	public boolean hasRecipe() {
		return this.recipeHelper.hasRecipe();
	}
	
	public boolean tryCraft(@Nullable EntityPlayer player) {
		return this.recipeHelper.tryCraft(player);
	}
	
	public int getRecipeCount() {
		return this.recipeHelper.getRecipeCount();
	}
	
	public void dispense(ItemStack itemstack, boolean silent) {
		EnumFacing enumfacing = this.getOutputFace();
		BlockPos blockpos = this.pos.offset(enumfacing);
		IInventory iinventory = TileEntityHopper.getInventoryAtPosition(this.world, (double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
		BlockSourceImpl blocksrc = new BlockSourceImpl(this.world, this.pos);

		if (iinventory == null)
		{
			this.dispenseHelper.dispenseFancy(blocksrc, itemstack, silent);
		}
		else
		{
			ItemStack itemstack1 = TileEntityHopper.putStackInInventoryAllSlots(this, iinventory, itemstack.copy().splitStack(itemstack.getCount()), enumfacing.getOpposite());

			if (!itemstack1.isEmpty()) {
				this.dispenseHelper.dispenseFancy(blocksrc, itemstack1, silent);
			}
		}
	}

	public void dispenseResult() {
		ItemStack itemstack = this.getStackInSlot(0);

		if (!itemstack.isEmpty())
		{
			this.dispense(itemstack, false);
			this.markDirty();
		}
	}
	
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}
	
	@Override
	public void update() {
		if (!this.hasWorld() || this.world.isRemote || !BlockCraftingAutomat.isActive(this.getBlockMetadata())) {
			return;
		}
		
		this.ticksActive++;
		
		if ((this.ticksActive <= CRAFTING_TICKS && !this.hasRecipe()) || this.ticksActive >= CRAFTING_TICKS + COOLDOWN_TICKS) {
			this.ticksActive = 0;
			this.world.setBlockState(this.pos, this.world.getBlockState(this.pos).withProperty(BlockCraftingAutomat.ACTIVE, Boolean.valueOf(false)), 2);
		}
		else if (this.ticksActive == CRAFTING_TICKS) {
			this.tryCraft(null);
			this.dispenseResult();
			this.markDirty();
		}
	}

	@Override
	public int getSizeInventory() {
		return this.buffer.size() + this.craftMatrix.getSizeInventory() + this.craftResult.getSizeInventory();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.buffer) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return this.craftMatrix.isEmpty() && this.craftResult.isEmpty();
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		if (index == 0) {
			return this.craftResult.getStackInSlot(0);
		}
		else if (index < 10) {
			return this.craftMatrix.getStackInSlot(index - 1);
		}
		else {
			return this.buffer.get(index - 10);
		}
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		if (index == 0) {
			return this.craftResult.decrStackSize(0, count);
		}
		else if (index < 10) {
			return this.craftMatrix.decrStackSize(index - 1, count);
		}
		else {
			ItemStack stack = ItemStackHelper.getAndSplit(this.buffer, index - 10, count);
			
			if (!stack.isEmpty()) {
				this.markDirty();
			}
			
			return stack;
		}
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		if (index == 0) {
			return this.craftResult.removeStackFromSlot(0);
		}
		else if (index < 10) {
			return this.craftMatrix.removeStackFromSlot(index - 1);
		}
		else {
			ItemStack stack = ItemStackHelper.getAndRemove(this.buffer, index - 10);
			
			if (!stack.isEmpty()) {
				this.markDirty();
			}
			
			return stack;
		}
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		if (index == 0) {
			this.craftResult.setInventorySlotContents(0, stack);
		}
		else if (index < 10) {
			this.craftMatrix.setInventorySlotContents(index - 1, stack);
		}
		else {
			if (index - 10 >= 0 && index - 10 < this.buffer.size()) {
				this.buffer.set(index - 10, stack);
				this.markDirty();
			}
		}
	}

	@Override
	public void openInventory(EntityPlayer player) {}

	@Override
	public void closeInventory(EntityPlayer player) {}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		if (this.world.getTileEntity(this.pos) != this) {
			return false;
		}
		else {
			// craftMatrix and craftResult return true anyway
			return player.getDistanceSq((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.5D, (double)this.pos.getZ() + 0.5D) <= 64.0D;
		}
	}

	@Override
	public void markDirty() {
		//this.craftMatrix.markDirty();
		//this.craftResult.markDirty();
		this.updateRecipe();
		super.markDirty();
	}
	
	//Update the recipe and result slot
	private void updateRecipe() {
		if (this.hasWorld()) {
			ItemStack itemstack = ItemStack.EMPTY;
			IRecipe irecipe = CraftingManager.findMatchingRecipe(this.craftMatrix, this.world);
			this.craftResult.setRecipeUsed(irecipe);
			
			if (irecipe != null && (irecipe.isDynamic() || !this.world.getGameRules().getBoolean("doLimitedCrafting"))) {
				itemstack = irecipe.getCraftingResult(this.craftMatrix);
			}
			
			this.setInventorySlotContents(0, itemstack);
		}
	}

	// ONLY used for automation. For GUIs Slot checks are used
	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return index != 0;
	}

	@Override
	public int getField(int id) {
		return this.ticksActive;
	}

	@Override
	public void setField(int id, int value) {
        this.ticksActive = value;
	}

	@Override
	public int getFieldCount() {
		return 1;
	}

	@Override
	public void clear() {
		this.buffer.clear();
		this.craftMatrix.clear();
		this.craftResult.clear();
	}

	@Override
	public ITextComponent getDisplayName() {
		return (ITextComponent)(this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName(), new Object[0]));
	}

	@Override
	public String getName() {
		return this.hasCustomName() ? this.customName : "container." + BlockCraftingAutomat.DEFAULT.toString();
	}

	@Override
	public boolean hasCustomName() {
		return this.customName != null && !this.customName.isEmpty();
	}

	public void setCustomName(String name) {
		this.customName = name;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		if (side == this.getOutputFace()) {
			return SLOTS_RESULT;
		}
		else {
			return side == EnumFacing.DOWN ? SLOTS_BUFFER_REV : SLOTS_BUFFER;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);

		NonNullList<ItemStack> items = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(compound, items);

		for (int i = 0; i < items.size(); i++) {
			this.setInventorySlotContents(i, items.get(i));
		}

		if (compound.hasKey("CustomName", 8)) {
			this.customName = compound.getString("CustomName");
		}

		this.ticksActive = compound.getInteger("TicksActive");
		this.markDirty();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);

		NonNullList<ItemStack> items = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);

		for (int i = 0; i < items.size(); i++) {
			items.set(i, this.getStackInSlot(i));
		}

		ItemStackHelper.saveAllItems(compound, items);

		if (this.hasCustomName()) {
			compound.setString("CustomName", this.customName);
		}

		compound.setInteger("TicksActive", (short) this.ticksActive);

		return compound;
	}

	@Override
	public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
		return this.isItemValidForSlot(index, itemStackIn);
	}

	@Override
	public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
		return direction != this.getOutputFace();
	}
	
	@Override
	public void onLoad() {
		this.markDirty();
	}
	
	private EnumFacing getOutputFace() {
		IBlockState state = this.world.getBlockState(this.pos);
		return state.getBlock() instanceof BlockCraftingAutomat ? state.getValue(BlockCraftingAutomat.FACING) : EnumFacing.NORTH;
	}
	
    public int findSlot(ItemStack stack) {
    	// Eats items in reverse order
        for (int i : SLOTS_BUFFER_REV) {
            ItemStack itemstack = this.getStackInSlot(i);

            if (!itemstack.isEmpty() && canCombine(itemstack, stack)) {
            		//&& !itemstack.isItemDamaged()
            		//&& !itemstack.isItemEnchanted()
            		//&& !itemstack.hasDisplayName()) {
                return i;
            }
        }

        System.out.println("This is probably not good");
        return -1;
    }
	
    protected static boolean canCombine(ItemStack stack1, ItemStack stack2) {
    	return ItemStack.areItemsEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }
	
    IItemHandler handlerResult;
    IItemHandler handlerBufferRev = new SidedInvWrapper(this, net.minecraft.util.EnumFacing.DOWN);
    IItemHandler handlerBuffer = new SidedInvWrapper(this, net.minecraft.util.EnumFacing.UP);

    private IItemHandler getOutputWrapper() {
    	IItemHandler temp = new SidedInvWrapper(this, this.getOutputFace());
    	
    	if (this.handlerResult == null || !this.handlerResult.equals(temp)) {
    		this.handlerResult = temp;
    	}
    	
    	return this.handlerResult;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == this.getOutputFace()) {
                return (T) this.getOutputWrapper();
            }
            else if (facing == EnumFacing.DOWN) {
                return (T) this.handlerBufferRev;
            }
            else {
                return (T) this.handlerBuffer;
            }
        }
        return super.getCapability(capability, facing);
    }
    
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

	private class DispenseHelper extends BehaviorDefaultDispenseItem {

		public final ItemStack dispenseFancy(IBlockSource source, ItemStack stack, boolean silent) {
			if (!silent) {
				this.playDispenseSound(source);
				this.spawnDispenseParticles(source, (EnumFacing)source.getBlockState().getValue(BlockCraftingAutomat.FACING));
			}
			return this.dispenseStack(source, stack);
		}

		@Override
		protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
			EnumFacing enumfacing = (EnumFacing)source.getBlockState().getValue(BlockCraftingAutomat.FACING);
			IPosition iposition = BlockCraftingAutomat.getDispensePosition(source);
			ItemStack itemstack = stack.splitStack(stack.getCount()); //is empty
			doDispense(source.getWorld(), itemstack, 6, enumfacing, iposition);
			return ItemStack.EMPTY;
		}

	}
	
	private class RecipeHelper {
		
		private RecipeItemHelper itemHelper = new RecipeItemHelper();
		
		private TileEntityCraftingAutomat inventory;
	    private InventoryCraftResult craftResult;
	    private InventoryCrafting craftMatrix;
	    
	    public RecipeHelper(TileEntityCraftingAutomat inv, InventoryCraftResult res, InventoryCrafting mat) {
	    	this.inventory = inv;
	    	this.craftResult = res;
	    	this.craftMatrix = mat;
	    }
	    
	    public void updateHelper() {
	    	this.itemHelper.clear();
	    	
	        for (ItemStack itemstack : this.inventory.buffer) {
	            this.itemHelper.accountStack(itemstack);
	        }
	    }
	    
	    public int getRecipeCount() {
    		this.updateHelper();
	    	return this.hasRecipe() ? this.itemHelper.getBiggestCraftableStack(this.getRecipe(), null) : 0;
	    }
	    
	    public boolean tryCraft(@Nullable EntityPlayer player) {
	    	if (this.hasRecipe()) {
	    		this.updateHelper();
	    		
		    	IntList intlist = new IntArrayList();
		    	List<Integer> slots;
		    	
		    	NonNullList<ItemStack> remainingItems = this.getRecipe().getRemainingItems(this.craftMatrix);
		    	List<ItemStack> reducedStacks = this.getFilledSlots().stream().map(remainingItems::get).collect(Collectors.toList());
		    	
	    		if (this.itemHelper.canCraft(this.getRecipe(), intlist) && !this.getRecipe().isDynamic()) {
	    			slots = this.craftFromBuffer(intlist);
	    		}
	    		else {
	    			slots = this.craftFromMatrix();
	    		}
	    		
	    		this.handleContainerItems(slots.iterator(), reducedStacks.iterator(), player);
	    	}
	    	
	    	return this.hasRecipe();
	    }
	    
	    private List<Integer> getFilledSlots() {
	    	List<Integer> slots = new ArrayList<Integer>();
	    	
	    	for (int i = 0; i < this.craftMatrix.getSizeInventory(); i++) {
	    		ItemStack itemstack = this.craftMatrix.getStackInSlot(i);
	    		
	    		if (!itemstack.isEmpty()) {
	    			slots.add(i);
	    		}
	    	}
	    	
	    	return slots;
	    }
	    
	    private void handleContainerItems(Iterator<Integer> slots, Iterator<ItemStack> remaining, @Nullable EntityPlayer player) {
	    	while(remaining.hasNext()) {
	    		ItemStack stack = remaining.next();
	    		int slot = slots.next();
	    		
	    		if (!stack.isEmpty() && slot != -1) {
	    			ItemStack slotStack = this.inventory.getStackInSlot(slot);
	    			
	    			if (slotStack.isEmpty()) {
	                    this.inventory.setInventorySlotContents(slot, stack);
	                }
	                else if (canCombine(slotStack, stack) && stack.getMaxStackSize() <= stack.getCount() + slotStack.getCount())
	                {
	                	stack.grow(slotStack.getCount());
	                    this.inventory.setInventorySlotContents(slot, stack);
	                }
	                else
	                {
	                	ItemStack notput = ItemHandlerHelper.insertItemStacked(this.inventory.handlerBuffer, stack, false);
	        			if (!notput.isEmpty()) {
	        				if (player != null) {
	        					ItemHandlerHelper.giveItemToPlayer(player, notput);
	        				}
	        				else {
	        					this.inventory.dispense(notput, true);
	        				}
	        			}
	                }
	    		}
	    	}
	    }
	    
	    private List<Integer> craftFromMatrix() {
	    	List<Integer> slots = new ArrayList<Integer>();
	    	
	    	for (int i = 0; i < this.craftMatrix.getSizeInventory(); i++) {
	    		ItemStack itemstack = this.craftMatrix.getStackInSlot(i);
	    		
	    		if (!itemstack.isEmpty()) {
	    			slots.add(i + 1);
	    			this.eatFromSlot(i + 1);
	    		}
	    	}
	    	
	    	return slots;
	    }
	    
	    private List<Integer> craftFromBuffer(IntList intlist) {
	    	Iterator<Integer> iterator = intlist.iterator();
	    	List<Integer> slots = new ArrayList<Integer>();
	    	
	    	while (iterator.hasNext()) {
	    		ItemStack itemstack = RecipeItemHelper.unpack(((Integer)iterator.next()).intValue());
	    		
	    		if (!itemstack.isEmpty()) {
    				int i = this.inventory.findSlot(itemstack);
		    		slots.add(i);

    		    	if (i != -1) {
    		    		this.eatFromSlot(i);
    		    	}
    			}
	    	}
	    	
	    	return slots;
	    }
	    
	    private void eatFromSlot(int i) {
    		ItemStack itemstack = this.inventory.getStackInSlot(i).copy();

    		if (itemstack.getCount() > 1) {
    			this.inventory.decrStackSize(i, 1);
    		}
    		else {
    			this.inventory.removeStackFromSlot(i);
    		}
	    }
	    
	    // Because I'm lazy
		private IRecipe getRecipe() {
			return this.craftResult.getRecipeUsed();
		}
	    
		private boolean hasRecipe() {
			return this.getRecipe() != null;
		}
	}

}
