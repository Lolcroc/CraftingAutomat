package lolcroc.craftingautomat;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
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
import net.minecraftforge.fml.loading.StringUtils;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.EmptyHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class CraftingAutomatTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity, IRecipeHolder {

    private ITextComponent customName;
    private LockCode lock = LockCode.EMPTY_CODE;

    private RecipeItemHelper itemHelper = new RecipeItemHelper();
    private Optional<ICraftingRecipe> recipeUsed = Optional.empty();
    private int ticksActive;
    IntReferenceHolder ticksHolder = new IntReferenceHolder() {
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
        NONE, READY, MISSING, INVALID;

        private final List<ITextComponent> displayTags;
        private static final CraftingFlag[] VALUES = CraftingFlag.values();

        CraftingFlag() {
            List<ITextComponent> list = Lists.newArrayList();
            list.add((new TranslationTextComponent(toString())).func_240699_a_(TextFormatting.GRAY));
            displayTags = list;
        }

        public int getIndex() {
            return ordinal();
        }

        public List<ITextComponent> getDisplayTags() {
            return displayTags;
        }

        public static CraftingFlag fromIndex(int idx) {
            return VALUES[idx];
        }

        public static CraftingFlag getNewFlag(Optional<ICraftingRecipe> recipe, RecipeItemHelper helper) {
            return recipe.map(r -> r.isDynamic() ? INVALID :
                    ((helper.getBiggestCraftableStack(r, null) > 0) ? READY : MISSING)).orElse(NONE);
        }

        @Override
        public String toString() {
            return "container." + CraftingAutomatBlock.REGISTRY_NAME.toString() + ".flag." + StringUtils.toLowerCase(name());
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

            if ((ticksActive <= CraftingAutomatConfig.CRAFTING_TICKS.get() && !isReady()) ||
                    ticksActive >= CraftingAutomatConfig.CRAFTING_TICKS.get() + CraftingAutomatConfig.COOLDOWN_TICKS.get()) {
                ticksActive = 0;
                world.setBlockState(pos, getBlockState().with(CraftingAutomatBlock.ACTIVE, Boolean.FALSE));
            } else if (ticksActive == CraftingAutomatConfig.CRAFTING_TICKS.get()) {
                resultHandler.ifPresent(h -> {
                    // Copy because crafting from buffer doesn't update the recipe output slot
                    ItemStack stack = h.getStackInSlot(0).copy();
                    if (!stack.isEmpty()) ResultHandler.outputStack(this, stack, false);
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
            matrixWrapper.ifPresent(w -> {
                recipeUsed = world.getRecipeManager().getRecipe(IRecipeType.CRAFTING, w, world)
                        .filter(r -> canUseRecipe(world, null, r)); // Set new recipe or null if missing/can't craft
                resultHandler.ifPresent(h -> h.setStackInSlot(0, recipeUsed.map(r ->
                        r.getCraftingResult(w)).orElse(ItemStack.EMPTY))); // Update result slot
            });
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

    // Read NBT
    @Override
    public void func_230337_a_(BlockState state, CompoundNBT compound) {
        super.func_230337_a_(state, compound);

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
            customName = ITextComponent.Serializer.func_240643_a_(compound.getString("CustomName")); // fromJson
        }

        ticksActive = compound.getInt("TicksActive");
        lock = LockCode.read(compound);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);

        bufferHandler.ifPresent(h -> compound.put("Buffer", ((INBTSerializable<CompoundNBT>) h).serializeNBT()));
        matrixHandler.ifPresent(h -> compound.put("Matrix", ((INBTSerializable<CompoundNBT>) h).serializeNBT()));

        if (hasCustomName()) {
            compound.putString("CustomName", ITextComponent.Serializer.toJson(customName));
        }

        compound.putInt("TicksActive", ticksActive);
        lock.write(compound);

        return compound;
    }

    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (!removed && capability == ITEM_HANDLER_CAPABILITY) {
            if (facing == null) {
                return combinedHandler.cast(); // If null side; return normal buffer + crafting matrix
            }
            else if (facing == getBlockState().get(CraftingAutomatBlock.FACING)) {
                return resultHandler.cast(); // Return the result face as a 'read-only'
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
            boolean ready = isReady();
            // Need to resolve this value now, otherwise the list will be empty after crafting from matrix
            NonNullList<ItemStack> remainingStacks = matrixWrapper.map(recipe::getRemainingItems)
                    .orElse(NonNullList.withSize(0, ItemStack.EMPTY));

            // Craft from buffer
            if (ready) {
                reversedBufferHandler.ifPresent(h -> {
                    recipe.getIngredients().forEach(i -> {
                        for (int j = 0; j < h.getSlots(); j++) {
                            if (i.test(h.getStackInSlot(j))) {
                                h.extractItem(j, 1, false);
                                break;
                            }
                        }
                    });
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
            IntStream.range(0, remainingStacks.size())
                    .mapToObj(i -> {
                        ItemStack stack = remainingStacks.get(i);
                        return ready ? stack : matrixHandler.map(h -> h.insertItem(i, stack, false)).orElse(stack);
                    }) // Insert back the corresponding matrix slot if crafted from there
                    .filter(stack -> !stack.isEmpty())
                    .map(stack -> reversedBufferHandler.map(h ->
                            ItemHandlerHelper.insertItemStacked(h, stack, false)).orElse(stack)) // Insert in buffer
                    .filter(stack -> !stack.isEmpty())
                    .forEach(stack -> {
                        if (player != null) {
                            ItemHandlerHelper.giveItemToPlayer(player, stack); // Give to player if present
                        }
                        else {
                            ResultHandler.outputStack(this, stack, true); // Output from autocrafter
                        }
                    });
        });
    }
}