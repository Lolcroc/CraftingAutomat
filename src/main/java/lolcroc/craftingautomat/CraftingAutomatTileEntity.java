package lolcroc.craftingautomat;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.LockCode;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.EmptyHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class CraftingAutomatTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity, IRecipeHolder {

    private static final int CRAFTING_TICKS = CraftingAutomatConfig.CRAFTING_TICKS.get();
    private static final int COOLDOWN_TICKS = CraftingAutomatConfig.COOLDOWN_TICKS.get();

	private ITextComponent customName;
	private LockCode lock = LockCode.EMPTY_CODE;

	private RecipeItemHelper itemHelper = new RecipeItemHelper();
	private Optional<ICraftingRecipe> recipeUsed = Optional.empty();
	private int ticksActive;
	protected IntReferenceHolder ticksHolder = new IntReferenceHolder() {
		@Override
		public int get() {
			return ticksActive;
		}

		@Override
		public void set(int value) {
			ticksActive = value;
		}
	};

	enum CraftingFlag {
		NONE("none"),
		READY("ready"),
		MISSING("missing"),
		INVALID( "invalid");

		private final ITextComponent displayName;
		private static final CraftingFlag[] VALUES = CraftingFlag.values();

		CraftingFlag(String name) {
			displayName = new TranslationTextComponent("container."
					+ CraftingAutomatBlock.REGISTRY_NAME.toString()
					+ ".flag." + name);
		}

		public int getIndex() {
			return ordinal();
		}

		public ITextComponent getDisplayName() {
			return displayName.applyTextStyle(TextFormatting.GRAY);
		}

		public static CraftingFlag fromIndex(int idx) {
			return VALUES[idx];
		}

		public static CraftingFlag getNewFlag(Optional<ICraftingRecipe> recipe, RecipeItemHelper helper) {
			if (recipe.isPresent()) {
				ICraftingRecipe r = recipe.get();
				return r.isDynamic() ? INVALID : ((helper.getBiggestCraftableStack(r, null) > 0) ? READY : MISSING);
			}
			else {
				return NONE;
			}
		}
	};

	private CraftingFlag craftingFlag = CraftingFlag.NONE;
	protected IntReferenceHolder craftingFlagHolder = new IntReferenceHolder() {
		@Override
		public int get() {
			return craftingFlag.getIndex();
		}

		@Override
		public void set(int value) {
			craftingFlag = CraftingFlag.fromIndex(value);
		}
	};

	private final static EmptyHandler EMPTYHANDLER = new EmptyHandler();

	// Inventory handlers for buffer, matrix and result slot separate
	protected LazyOptional<IItemHandlerModifiable> bufferHandler = LazyOptional.of(() -> new BufferHandler(this));
	protected LazyOptional<IItemHandlerModifiable> matrixHandler = LazyOptional.of(() -> new MatrixHandler(this));
	protected LazyOptional<IItemHandlerModifiable> resultHandler = LazyOptional.of(ResultHandler::new);

	// Combined handler when face not specified (e.g. when breaking block)
	protected LazyOptional<IItemHandlerModifiable> combinedHandler = LazyOptional.of(() ->
			new CombinedInvWrapper(matrixHandler.orElse(EMPTYHANDLER), bufferHandler.orElse(EMPTYHANDLER)));
	// Reversed inventory wrapper for buffer access from bottom side
	protected LazyOptional<IItemHandler> reversedBufferHandler = LazyOptional.of(() ->
			new ReversedInvWrapper(bufferHandler.orElse(EMPTYHANDLER)));
	// Wrapper for using recipe methods
	protected LazyOptional<CraftingInventory> matrixWrapper = LazyOptional.of(() ->
			new CraftingInventoryWrapper(matrixHandler.orElse(EMPTYHANDLER)));

	public CraftingAutomatTileEntity() {
		super(CraftingAutomat.TileEntityTypes.autocrafter);
	}

	@Override
	public void tick() {
		if (!world.isRemote && getBlockState().get(CraftingAutomatBlock.ACTIVE)) {
			ticksActive++;

			if ((ticksActive <= CRAFTING_TICKS && !isReady()) || ticksActive >= CRAFTING_TICKS + COOLDOWN_TICKS) {
				ticksActive = 0;
				world.setBlockState(pos, getBlockState().with(CraftingAutomatBlock.ACTIVE, Boolean.FALSE));
			} else if (ticksActive == CRAFTING_TICKS) {
				resultHandler.ifPresent(h -> {
					ItemStack stack = h.getStackInSlot(0);
					if (!stack.isEmpty()) ResultHandler.outputStack(this, stack.copy(), false);
				});
				consumeIngredients(null);
			}
			markDirty();
		}
	}

	public boolean isReady() {
		return craftingFlag == CraftingFlag.READY;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		updateRecipe();
	}

	public void updateRecipe() {
		if (this.hasWorld()) {
			AtomicReference<ItemStack> itemstack = new AtomicReference<>(ItemStack.EMPTY);

			matrixWrapper.ifPresent(w -> {
				Optional<ICraftingRecipe> optional = world.getRecipeManager().getRecipe(IRecipeType.CRAFTING, w, world);

				if (optional.isPresent()) {
					ICraftingRecipe recipe = optional.get();

					if (canUseRecipe(world, null, recipe)) {
						setRecipeUsed(recipe);
						itemstack.set(recipe.getCraftingResult(w));
					}
					else {
						setRecipeUsed(null);
					}
				}
				else {
					setRecipeUsed(null);
				}
			});
			resultHandler.ifPresent(h -> h.setStackInSlot(0, itemstack.get()));
			craftingFlag = CraftingFlag.getNewFlag(recipeUsed, itemHelper);
		}
//		CraftingAutomat.LOGGER.warn("Did update recipe");
	}

	@Override
	public void setRecipeUsed(@Nullable IRecipe<?> recipe) {
		recipeUsed = Optional.ofNullable((ICraftingRecipe) recipe);
	}

	@Nullable
	@Override
	public IRecipe<?> getRecipeUsed() {
		return recipeUsed.orElse(null);
	}

	// Without player check and without set method
	@Override
	public boolean canUseRecipe(World worldIn, @Nullable ServerPlayerEntity player, IRecipe<?> recipe) {
		return !worldIn.getGameRules().getBoolean(GameRules.DO_LIMITED_CRAFTING) || recipe.isDynamic();
	}

	// Fired when the buffer changes
	public void updateHelper() {
		bufferHandler.ifPresent(h -> ((BufferHandler) h).fillStackedContents(itemHelper));
		craftingFlag = CraftingFlag.getNewFlag(recipeUsed, itemHelper);
//		CraftingAutomat.LOGGER.warn("Did update helper");
	}

	public int getRecipeCount() {
		return recipeUsed.map(r -> itemHelper.getBiggestCraftableStack(r, null)).orElse(0);
	}

	private static final ITextComponent DEFAULT_NAME = new TranslationTextComponent("container." + CraftingAutomatBlock.REGISTRY_NAME.toString());

	@Nonnull
	@Override
	public ITextComponent getDisplayName() {
		return this.hasCustomName() ? this.customName : DEFAULT_NAME;
	}

	@Override
	public Container createMenu(int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player) {
		return LockableTileEntity.canUnlock(player, this.lock, this.getDisplayName()) ? new CraftingAutomatContainer(id, inventory, this) : null;
	}

	public boolean hasCustomName() {
		return this.customName != null;
	}

	public void setCustomName(@Nullable ITextComponent name) {
		this.customName = name;
	}

	@Override
	public void read(CompoundNBT compound) {
		super.read(compound);

		// Backwards compatibility
		if (!compound.getList("Items", 10).isEmpty()) {
			NonNullList<ItemStack> items = NonNullList.<ItemStack>withSize(19, ItemStack.EMPTY);
			ItemStackHelper.loadAllItems(compound, items);

			for(int i = 1; i < items.size(); i++) { // Skip result slot
				int finalI = i;
				combinedHandler.ifPresent(h -> h.setStackInSlot(finalI - 1, items.get(finalI)));
			}
		}
		else {
			bufferHandler.ifPresent(h -> ((INBTSerializable<CompoundNBT>) h).deserializeNBT(compound.getCompound("Buffer")));
			matrixHandler.ifPresent(h -> ((INBTSerializable<CompoundNBT>) h).deserializeNBT(compound.getCompound("Matrix")));
		}

		if (compound.contains("CustomName", 8)) {
			this.customName = ITextComponent.Serializer.fromJson(compound.getString("CustomName"));
		}

		this.ticksActive = compound.getInt("TicksActive");
		this.lock = LockCode.read(compound);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		super.write(compound);

        bufferHandler.ifPresent(h -> compound.put("Buffer", ((INBTSerializable<CompoundNBT>) h).serializeNBT()));
        matrixHandler.ifPresent(h -> compound.put("Matrix", ((INBTSerializable<CompoundNBT>) h).serializeNBT()));

		if (this.hasCustomName()) {
			compound.putString("CustomName", ITextComponent.Serializer.toJson(this.customName));
		}

		compound.putInt("TicksActive", (short) this.ticksActive);
		this.lock.write(compound);
		
		return compound;
	}

	@CapabilityInject(IItemHandler.class)
	public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;

    @Nonnull
	@Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
		if (!removed && capability == ITEM_HANDLER_CAPABILITY && facing != getBlockState().get(CraftingAutomatBlock.FACING)) { // Dont return the result face
        	if (facing == null) {
        	    return combinedHandler.cast(); // If null side; return normal buffer + crafting matrix
			}
            else if (facing == Direction.DOWN) {
                return reversedBufferHandler.cast(); // If down face; return buffer reversed
            }
            else {
                return bufferHandler.cast(); // If any other face; return normal buffer
            }
        }
        return super.getCapability(capability, facing);
    }

	public void consumeIngredients(@Nullable PlayerEntity player) {
		recipeUsed.ifPresent(recipe -> {
			IntList ingredients = new IntArrayList();
			NonNullList<ItemStack> remainingStacks = matrixWrapper.map(recipe::getRemainingItems)
					.orElse(NonNullList.withSize(1, ItemStack.EMPTY));

			// Craft from buffer
			this.itemHelper.canCraft(recipe, ingredients); // Populate the ingredients list
			if (isReady()) {
				Iterator<ItemStack> stacks = ingredients.stream().map(RecipeItemHelper::unpack).iterator();

				// Consume in reversed order
				reversedBufferHandler.ifPresent(h -> {
					while (stacks.hasNext()) {
						ItemStack stack = stacks.next();

						for (int i = 0; i < h.getSlots(); i++) {
							ItemStack bufferstack = h.getStackInSlot(i);

							if (ItemHandlerHelper.canItemStacksStack(bufferstack, stack)) {
								h.extractItem(i, 1, false);
								break;
							}
						}
					}
				});
			}
			// Craft from matrix
			else {
				matrixHandler.ifPresent(h -> {
					for (int i = 0; i < h.getSlots(); i++) {
						h.extractItem(i, 1, false);
					}
				});
			}

			// Handle container items
			for (int i = 0; i < remainingStacks.size(); i++) {
				ItemStack stack = remainingStacks.get(i);
				if (stack.isEmpty()) {
					continue;
				}

				int finalI = i;

				// Only merge if all items can go in
				if (this.matrixHandler.map(h -> h.insertItem(finalI, stack, true).isEmpty()).orElse(true)) {
					this.matrixHandler.ifPresent(h -> h.insertItem(finalI, stack, false));
				}
				else { // Try to put in the buffer
					this.reversedBufferHandler.ifPresent(h -> {
						ItemStack notput = ItemHandlerHelper.insertItemStacked(h, stack, false);

						if (!notput.isEmpty()) {
							if (player != null) {
								ItemHandlerHelper.giveItemToPlayer(player, notput);
							}
							else {
								ResultHandler.outputStack(this, notput, true);
							}
						}
					});
				}
			}
		});
	}
}