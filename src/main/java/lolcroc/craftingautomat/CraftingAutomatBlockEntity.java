package lolcroc.craftingautomat;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class CraftingAutomatBlockEntity extends BlockEntity implements MenuProvider, RecipeHolder, Clearable {

    protected Component customName;
    protected LockCode lock = LockCode.NO_LOCK;

    protected StackedContents itemHelper = new StackedContents();
    protected CraftingRecipe recipeUsed;
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

        public static CraftingFlag getNewFlag(CraftingRecipe recipe, StackedContents helper) {
            return recipe != null ? (recipe.isSpecial() ? INVALID : (helper.getBiggestCraftableStack(recipe, null) > 0 ? READY : MISSING)) : NONE;
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

    // Inventory handlers for buffer, matrix and result slot separate
    private final BufferHandler bufferHandler = new BufferHandler(this);
    protected final LazyOptional<BufferHandler> bufferOptional = LazyOptional.of(() -> bufferHandler);

    private final MatrixHandler matrixHandler = new MatrixHandler(this);
    protected final LazyOptional<MatrixHandler> matrixOptional = LazyOptional.of(() -> matrixHandler);

    private final ResultHandler resultHandler = new ResultHandler();
    protected final LazyOptional<ResultHandler> resultOptional = LazyOptional.of(() -> resultHandler);

    // Combined handler when face not specified (e.g. when breaking block)
    private final IItemHandlerModifiable combinedHandler = new CombinedInvWrapper(matrixHandler, bufferHandler);
    protected final LazyOptional<IItemHandlerModifiable> combinedOptional = LazyOptional.of(() -> combinedHandler);
    // Reversed inventory wrapper for buffer access from bottom side

    private final IItemHandler reversedBufferHandler = new ReversedInvWrapper(bufferHandler);
    protected final LazyOptional<IItemHandler> reversedBufferOptional = LazyOptional.of(() -> reversedBufferHandler);
    // Wrapper for using recipe methods

    private final CraftingContainer matrixWrapperHandler = new CraftingInventoryWrapper(matrixHandler);
    protected final LazyOptional<CraftingContainer> matrixWrapperOptional = LazyOptional.of(() -> matrixWrapperHandler);

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
            // Copy because crafting from buffer doesn't update the recipe output slot
            ItemStack stack = entity.resultHandler.getStackInSlot(0).copy();
            if (!stack.isEmpty()) ResultHandler.outputStack(entity, stack, false);
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

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        bufferOptional.invalidate();
        matrixOptional.invalidate();
        resultOptional.invalidate();
        combinedOptional.invalidate();
        reversedBufferOptional.invalidate();
        matrixWrapperOptional.invalidate();
    }

    public void updateRecipe() {
        if (this.hasLevel() && !level.isClientSide) {
            Optional<CraftingRecipe> optional = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, matrixWrapperHandler, level);
            ItemStack output = ItemStack.EMPTY;
            CraftingRecipe newRecipe = null;

            if (optional.isPresent()) {
                CraftingRecipe recipe = optional.get();

                if (recipe.isSpecial() || !level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) || RecipesSavedData.computeIfAbsent(level.getServer()).contains(recipe)) {
                    output = recipe.assemble(matrixWrapperHandler);
                    newRecipe = recipe;
                }
            }

            recipeUsed = newRecipe;
            resultHandler.setStackInSlot(0, output);
            craftingFlag = CraftingFlag.getNewFlag(recipeUsed, itemHelper);
        }
//		CraftingAutomat.LOGGER.warn("Did update recipe");
    }

    // Casts update recipe automatically
    @Override
    public void clearContent() {
        for (int i = 0; i < combinedHandler.getSlots(); i++) {
            combinedHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void setRecipeUsed(@Nullable Recipe<?> recipe) {
        recipeUsed = (CraftingRecipe) recipe;
    }

    @Nullable
    @Override
    public Recipe<?> getRecipeUsed() {
        return recipeUsed;
    }
    // Fired when the buffer changes
    public void updateHelper() {
        bufferHandler.fillStackedContents(itemHelper);
        craftingFlag = CraftingFlag.getNewFlag(recipeUsed, itemHelper);
//		CraftingAutomat.LOGGER.warn("Did update helper");
    }

    public int getRecipeCount() {
        return recipeUsed != null ? itemHelper.getBiggestCraftableStack(recipeUsed, null) : 0;
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
        ((INBTSerializable<CompoundTag>) bufferHandler).deserializeNBT(compound.getCompound("Buffer"));
        ((INBTSerializable<CompoundTag>) matrixHandler).deserializeNBT(compound.getCompound("Matrix"));

        if (compound.contains("CustomName", 8)) {
            customName = Component.Serializer.fromJson(compound.getString("CustomName"));
        }

        ticksActive = compound.getInt("TicksActive");
        lock = LockCode.fromTag(compound);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);

        compound.put("Buffer", ((INBTSerializable<CompoundTag>) bufferHandler).serializeNBT());
        compound.put("Matrix", ((INBTSerializable<CompoundTag>) matrixHandler).serializeNBT());

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
                return combinedOptional.cast(); // If null side; return normal buffer + crafting matrix
            } else if (facing == getBlockState().getValue(CraftingAutomatBlock.FACING)) {
                return resultOptional.cast(); // Return the result face as a 'read-only'
            } else if (facing == Direction.DOWN) {
                return reversedBufferOptional.cast(); // If down face; return buffer reversed
            } else {
                return bufferOptional.cast(); // If any other face; return normal buffer
            }
        }
        return super.getCapability(capability, facing);
    }

    public void consumeIngredients(@Nullable Player player) {
        if (recipeUsed == null) return;

        boolean ready = isReady();
        // Need to resolve this value now, otherwise the list will be empty after crafting from matrix
        NonNullList<ItemStack> remainingStacks = recipeUsed.getRemainingItems(matrixWrapperHandler);

        // Craft from buffer
        if (ready) {
            recipeUsed.getIngredients().forEach(i -> {
                for (int j = 0; j < reversedBufferHandler.getSlots(); j++) {
                    if (i.test(reversedBufferHandler.getStackInSlot(j))) {
                        reversedBufferHandler.extractItem(j, 1, false);
                        break;
                    }
                }
            });
        }
        // Craft from matrix
        else {
            for (int i = 0; i < matrixHandler.getSlots(); i++) {
                matrixHandler.extractItem(i, 1, false);
            }
        }

        // Handle container items
        IntStream.range(0, remainingStacks.size())
                .mapToObj(i -> {
                    ItemStack stack = remainingStacks.get(i);
                    return ready ? stack : matrixHandler.insertItem(i, stack, false);
                }) // Insert back the corresponding matrix slot if crafted from there
                .filter(stack -> !stack.isEmpty())
                .map(stack -> ItemHandlerHelper.insertItemStacked(reversedBufferHandler, stack, false)) // Insert in buffer
                .filter(stack -> !stack.isEmpty())
                .forEach(stack -> {
                    if (player != null) {
                        ItemHandlerHelper.giveItemToPlayer(player, stack); // Give to player if present
                    } else {
                        ResultHandler.outputStack(this, stack, true); // Output from autocrafter
                    }
                });
    }
}