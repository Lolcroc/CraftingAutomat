package lolcroc.craftingautomat;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
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

public class CraftingAutomatBlockEntity extends BlockEntity implements MenuProvider, RecipeHolder, Clearable {

    protected Component customName;
    protected LockCode lock = LockCode.NO_LOCK;

    protected StackedContents itemHelper = new StackedContents();
    protected Optional<CraftingRecipe> recipeUsed = Optional.empty();
    protected int ticksActive;
    DataSlot ticksHolder = new DataSlot() {
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

        private final List<Component> displayTags;
        private static final CraftingFlag[] VALUES = CraftingFlag.values();

        CraftingFlag() {
            List<Component> list = Lists.newArrayList();
            list.add(Component.translatable(toString()).withStyle(ChatFormatting.GRAY));
            displayTags = list;
        }

        public int getIndex() {
            return ordinal();
        }

        public List<Component> getDisplayTags() {
            return displayTags;
        }

        public static CraftingFlag fromIndex(int idx) {
            return VALUES[idx];
        }

        public static CraftingFlag getNewFlag(Optional<CraftingRecipe> recipe, StackedContents helper) {
            return recipe.map(r -> r.isSpecial() ? INVALID :
                    ((helper.getBiggestCraftableStack(r, null) > 0) ? READY : MISSING)).orElse(NONE);
        }

        @Override
        public String toString() {
            return "container." + CraftingAutomatBlock.REGISTRY_NAME + ".flag." + StringUtils.toLowerCase(name());
        }
    }

    private CraftingFlag craftingFlag = CraftingFlag.NONE;
    protected DataSlot craftingFlagHolder = new DataSlot() {
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
    protected LazyOptional<CraftingContainer> matrixWrapper = LazyOptional.of(() ->
            new CraftingInventoryWrapper(matrixHandler.orElse(EMPTYHANDLER)));

    public CraftingAutomatBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingAutomat.AUTOCRAFTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CraftingAutomatBlockEntity entity) {
        entity.ticksActive++;

        if ((entity.ticksActive <= CraftingAutomatConfig.CRAFTING_TICKS.get() && !entity.isReady()) ||
                entity.ticksActive >= CraftingAutomatConfig.CRAFTING_TICKS.get() + CraftingAutomatConfig.COOLDOWN_TICKS.get()) {
            entity.ticksActive = 0;
            level.setBlockAndUpdate(pos, entity.getBlockState().setValue(CraftingAutomatBlock.ACTIVE, Boolean.FALSE));
        } else if (entity.ticksActive == CraftingAutomatConfig.CRAFTING_TICKS.get()) {
            entity.resultHandler.ifPresent(h -> {
                // Copy because crafting from buffer doesn't update the recipe output slot
                ItemStack stack = h.getStackInSlot(0).copy();
                if (!stack.isEmpty()) ResultHandler.outputStack(entity, stack, false);
            });
            entity.consumeIngredients(null);
        }
        entity.setChanged();
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
        if (this.hasLevel()) {
            matrixWrapper.ifPresent(w -> {
                recipeUsed = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, w, level)
                        .filter(r -> setRecipeUsed(level, null, r)); // Set new recipe or null if missing/can't craft
                resultHandler.ifPresent(h -> h.setStackInSlot(0, recipeUsed.map(r ->
                        r.assemble(w, level.registryAccess())).orElse(ItemStack.EMPTY))); // Update result slot
            });
            craftingFlag = CraftingFlag.getNewFlag(recipeUsed, itemHelper);
        }
//		CraftingAutomat.LOGGER.warn("Did update recipe");
    }

    @Override
    public void clearContent() {
        combinedHandler.ifPresent(h -> {
            for (int i = 0; i < h.getSlots(); i++) {
                h.setStackInSlot(i, ItemStack.EMPTY);
            }
        });
        updateRecipe();
    }

    @Override
    public void setRecipeUsed(@Nullable Recipe<?> recipe) {
        recipeUsed = Optional.ofNullable((CraftingRecipe) recipe);
    }

    @Nullable
    @Override
    public Recipe<?> getRecipeUsed() {
        return recipeUsed.orElse(null);
    }

    // Without player check and without set method
    @Override
    public boolean setRecipeUsed(Level level, @Nullable ServerPlayer player, Recipe<?> recipe) {
        return true;
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

    private static final Component DEFAULT_NAME = Component.translatable("container." + CraftingAutomatBlock.REGISTRY_NAME.toString());

    @Nonnull
    @Override
    public Component getDisplayName() {
        return this.hasCustomName() ? this.customName : DEFAULT_NAME;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return BaseContainerBlockEntity.canUnlock(player, this.lock, this.getDisplayName()) ? new CraftingAutomatContainer(id, inventory, this) : null;
    }

    public boolean hasCustomName() {
        return this.customName != null;
    }

    public void setCustomName(@Nullable Component name) {
        this.customName = name;
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);

        // Backwards compatibility
        if (!compound.getList("Items", 10).isEmpty()) {
            NonNullList<ItemStack> items = NonNullList.<ItemStack>withSize(19, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(compound, items);

            for (int i = 1; i < items.size(); i++) { // Skip result slot
                int finalI = i;
                combinedHandler.ifPresent(h -> h.setStackInSlot(finalI - 1, items.get(finalI)));
            }
        } else {
            bufferHandler.ifPresent(h -> ((INBTSerializable<CompoundTag>) h).deserializeNBT(compound.getCompound("Buffer")));
            matrixHandler.ifPresent(h -> ((INBTSerializable<CompoundTag>) h).deserializeNBT(compound.getCompound("Matrix")));
        }

        if (compound.contains("CustomName", 8)) {
            customName = Component.Serializer.fromJson(compound.getString("CustomName"));
        }

        ticksActive = compound.getInt("TicksActive");
        lock = LockCode.fromTag(compound);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);

        bufferHandler.ifPresent(h -> compound.put("Buffer", ((INBTSerializable<CompoundTag>) h).serializeNBT()));
        matrixHandler.ifPresent(h -> compound.put("Matrix", ((INBTSerializable<CompoundTag>) h).serializeNBT()));

        if (hasCustomName()) {
            compound.putString("CustomName", Component.Serializer.toJson(customName));
        }

        compound.putInt("TicksActive", ticksActive);
        lock.addToTag(compound);
    }

    private static final Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (!remove && capability == ITEM_HANDLER_CAPABILITY) {
            if (facing == null) {
                return combinedHandler.cast(); // If null side; return normal buffer + crafting matrix
            } else if (facing == getBlockState().getValue(CraftingAutomatBlock.FACING)) {
                return resultHandler.cast(); // Return the result face as a 'read-only'
            } else if (facing == Direction.DOWN) {
                return reversedBufferHandler.cast(); // If down face; return buffer reversed
            } else {
                return bufferHandler.cast(); // If any other face; return normal buffer
            }
        }
        return super.getCapability(capability, facing);
    }

    public void consumeIngredients(@Nullable Player player) {
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
                        } else {
                            ResultHandler.outputStack(this, stack, true); // Output from autocrafter
                        }
                    });
        });
    }
}