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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeHolder;
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
import java.util.stream.IntStream;

public class CraftingAutomatBlockEntity extends BlockEntity implements MenuProvider, RecipeHolder, Clearable {

    protected Component customName;
    protected LockCode lock = LockCode.NO_LOCK;

    protected StackedContents itemHelper = new StackedContents();
    protected CraftingRecipe recipeUsed;
    protected int ticksActive;

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

    protected CraftingFlag craftingFlag = CraftingFlag.NONE;

    protected int crafingTicks;  // ONLY used logical client
    protected int cooldownTicks;  // Only used logical client

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {  // Always on server
            switch (id) {
                case 0:
                    return craftingFlag.getIndex();
                case 1:
                    return ticksActive;
                case 2:
                    return getCraftingTicks();
                case 3:
                    return getCooldownTicks();
                default:
                    return 0;
            }
        }

        @Override
        public void set(int id, int value) {  // Always on client
            switch (id) {
                case 0:
                    craftingFlag = CraftingFlag.fromIndex(value);
                    break;
                case 1:
                    ticksActive = value;
                    break;
                case 2:
                    crafingTicks = value;
                    break;
                case 3:
                    cooldownTicks = value;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    // Inventory handlers for buffer, matrix and result slot separate
    protected final ItemHandlers.Buffer buffer = new ItemHandlers.Buffer(this);
    protected final ItemHandlers.Matrix matrix = new ItemHandlers.Matrix(this);
    protected final ItemHandlers.Result result = new ItemHandlers.Result();
    protected final IItemHandlerModifiable inventory = new CombinedInvWrapper(matrix, buffer);
    protected final IItemHandler reversedBuffer = new ItemHandlers.Reversed(buffer);
    protected final CraftingContainer matrixWrapper = new CraftingInventoryWrapper(matrix);

    protected final LazyOptional<ItemHandlers.Buffer> bufferOptional = LazyOptional.of(() -> buffer);
    protected final LazyOptional<ItemHandlers.Result> resultOptional = LazyOptional.of(() -> result);
    // Combined handler when face not specified (e.g. when breaking block)
    protected final LazyOptional<IItemHandlerModifiable> inventoryOptional = LazyOptional.of(() -> inventory);
    // Reversed inventory wrapper for buffer access from bottom side
    protected final LazyOptional<IItemHandler> reversedBufferOptional = LazyOptional.of(() -> reversedBuffer);
    // Wrapper for using recipe methods


    public CraftingAutomatBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingAutomat.AUTOCRAFTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static int getCraftingTicks() {
        return CraftingAutomatConfig.CRAFTING_TICKS.get();
    }

    public static int getCooldownTicks() {
        return CraftingAutomatConfig.COOLDOWN_TICKS.get();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CraftingAutomatBlockEntity entity) {
        entity.ticksActive++;

        if ((entity.ticksActive <= getCraftingTicks() && !entity.isReady()) ||
                entity.ticksActive >= getCraftingTicks() + getCooldownTicks()) {
            entity.ticksActive = 0;
            level.setBlockAndUpdate(pos, entity.getBlockState().setValue(CraftingAutomatBlock.ACTIVE, Boolean.FALSE));
        } else if (entity.ticksActive == CraftingAutomatConfig.CRAFTING_TICKS.get()) {
            // Copy because crafting from buffer doesn't update the recipe output slot
            ItemStack stack = entity.result.getStackInSlot(0).copy();
            if (!stack.isEmpty()) ItemHandlers.outputStack(entity, stack, false);
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
        updateHelper();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        bufferOptional.invalidate();
        resultOptional.invalidate();
        inventoryOptional.invalidate();
        reversedBufferOptional.invalidate();
    }

    public void updateRecipe() {
        if (level != null && !level.isClientSide) {
            recipeUsed = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, matrixWrapper, level)
                    .filter(recipe -> recipe.isSpecial()
                            || !level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING)
                            || RecipesSavedData.computeIfAbsent(level.getServer()).contains(recipe))
                    .orElse(null);
            craftingFlag = CraftingFlag.getNewFlag(recipeUsed, itemHelper);

            updateOutput();
            setChanged();
        }
    }

    public void updateOutput() {
        if (level != null && !level.isClientSide) {
            ItemStack output = recipeUsed != null ? recipeUsed.assemble(matrixWrapper) : ItemStack.EMPTY;
            result.setStackInSlot(0, output);
        }
    }

    // Calls update recipe automatically
    @Override
    public void clearContent() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
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
        if (level != null && !level.isClientSide) {
            buffer.fillStackedContents(itemHelper);
            craftingFlag = CraftingFlag.getNewFlag(recipeUsed, itemHelper);
            setChanged();
        }
    }

    public int getRecipeCount() {
        return recipeUsed != null ? itemHelper.getBiggestCraftableStack(recipeUsed, null) : 0;
    }

    private static final Component DEFAULT_NAME = Component.translatable("container.crafting");

    @Nonnull
    @Override
    public Component getDisplayName() {
        return this.hasCustomName() ? this.customName : DEFAULT_NAME;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return BaseContainerBlockEntity.canUnlock(player, this.lock, this.getDisplayName()) ? new CraftingAutomatMenu(id, inventory, this) : null;
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

        ((INBTSerializable<CompoundTag>) buffer).deserializeNBT(compound.getCompound("Buffer"));
        ((INBTSerializable<CompoundTag>) matrix).deserializeNBT(compound.getCompound("Matrix"));

        if (compound.contains("CustomName", 8)) {
            customName = Component.Serializer.fromJson(compound.getString("CustomName"));
        }

        ticksActive = compound.getInt("TicksActive");
        lock = LockCode.fromTag(compound);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);

        compound.put("Buffer", ((INBTSerializable<CompoundTag>) buffer).serializeNBT());
        compound.put("Matrix", ((INBTSerializable<CompoundTag>) matrix).serializeNBT());

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
                return inventoryOptional.cast(); // If null side; return normal buffer + crafting matrix
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
        NonNullList<ItemStack> remainingStacks = recipeUsed.getRemainingItems(matrixWrapper);

        // Craft from buffer
        if (ready) {
            recipeUsed.getIngredients().forEach(i -> {
                for (int j = 0; j < reversedBuffer.getSlots(); j++) {
                    if (i.test(reversedBuffer.getStackInSlot(j))) {
                        reversedBuffer.extractItem(j, 1, false);
                        break;
                    }
                }
            });
        }
        // Craft from matrix
        else {
            for (int i = 0; i < matrix.getSlots(); i++) {
                matrix.extractItem(i, 1, false);
            }
        }

        // Handle container items
        IntStream.range(0, remainingStacks.size())
                .mapToObj(i -> {
                    ItemStack stack = remainingStacks.get(i);
                    return ready ? stack : matrix.insertItem(i, stack, false);
                }) // Insert back the corresponding matrix slot if crafted from there
                .filter(stack -> !stack.isEmpty())
                .map(stack -> ItemHandlerHelper.insertItemStacked(reversedBuffer, stack, false)) // Insert in buffer
                .filter(stack -> !stack.isEmpty())
                .forEach(stack -> {
                    if (player != null) {
                        ItemHandlerHelper.giveItemToPlayer(player, stack); // Give to player if present
                    } else {
                        ItemHandlers.outputStack(this, stack, true); // Output from autocrafter
                    }
                });
    }
}