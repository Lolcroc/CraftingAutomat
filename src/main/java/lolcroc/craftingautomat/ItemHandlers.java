package lolcroc.craftingautomat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.PositionImpl;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Iterator;

public class ItemHandlers {
    private static final Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    protected static void dispense(Level level, BlockPos pos, Direction side, ItemStack stack, boolean silent) {
        double x = pos.getX() + 0.5D + 0.7D * (double)side.getStepX();
        double y = pos.getY() + 0.5D + 0.7D * (double)side.getStepY();
        double z = pos.getZ() + 0.5D + 0.7D * (double)side.getStepZ();

        // Lower the dispense position slightly when shooting from the side
        if (side.getAxis().isHorizontal()) {
            y -= 0.2D;
        }

        ItemStack itemstack = stack.split(stack.getCount()); //is empty afterwards
        DefaultDispenseItemBehavior.spawnItem(level, itemstack, 6, side, new PositionImpl(x, y, z));

        if (!silent) {
            level.levelEvent(1000, pos, 0); // Play dispense sound
            level.levelEvent(2000, pos, side.get3DDataValue()); // Spawn dispense particles
        }
    }

    private static ItemStack insertStack(ICapabilityProvider provider, Direction input, ItemStack stack) {
        return provider.getCapability(ITEM_HANDLER_CAPABILITY, input)
                .map(h -> ItemHandlerHelper.insertItemStacked(h, stack, false))
                .orElse(stack);
    }

    public static void outputStack(BlockEntity tile, ItemStack stack, boolean silent) {
        BlockPos tilepos = tile.getBlockPos();
        Level level = tile.getLevel();
        Direction outputface = tile.getBlockState().getValue(CraftingAutomatBlock.FACING);
        BlockPos targetpos = tilepos.relative(outputface);

        if (level.getBlockState(targetpos).hasBlockEntity()) {
            BlockEntity tileentity = level.getBlockEntity(targetpos);
            if (tileentity != null) {
                stack = insertStack(tileentity, outputface.getOpposite(), stack);
            }
        }
        else {
            // Only allow hopper/chest minecarts for now
            Iterator<Entity> invents = level.getEntities((Entity) null, new AABB(targetpos), e ->
                    e.getCapability(ITEM_HANDLER_CAPABILITY, outputface.getOpposite()).isPresent()
                            && e.isAlive() && (e instanceof AbstractMinecartContainer)).iterator();

            while (invents.hasNext() && !stack.isEmpty()) {
                stack = insertStack(invents.next(), outputface.getOpposite(), stack);
            }
        }

        // Dispense the remainder
        if (!stack.isEmpty()) {
            dispense(level, tilepos, outputface, stack, silent);
        }
    }

    public static class Result extends ItemStackHandler {

        // ONLY used for automation. For GUIs Slot checks are used
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return false;
        }

        // Make this handler 'read-only'
        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

    }

    public static class Matrix extends Unsafe {

        private final CraftingAutomatBlockEntity tile;

        public Matrix(CraftingAutomatBlockEntity te) {
            super(9);
            tile = te;
        }

        @Override
        protected void onContentsChanged(int slot) {
            tile.updateRecipe();
            tile.setChanged();
        }

        // No updateRecipe() in onLoad(): There is no world (and thus no recipemanager) during nbt deserialization :(
        // I fixed this in onLoad() of the TileEntity
    }

    public static class Buffer extends Unsafe implements StackedContentsCompatible {

        private final CraftingAutomatBlockEntity tile;

        public Buffer(CraftingAutomatBlockEntity te) {
            super(9);
            this.tile = te;
        }

        @Override
        protected void onContentsChanged(int slot) {
            tile.updateHelper();
            tile.setChanged();
        }

        @Override
        protected void onLoad() {
            tile.updateHelper();
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
            contents.clear();
            for(ItemStack itemstack : stacks) {
                contents.accountSimpleStack(itemstack);
            }
        }
    }

    public static class Unsafe extends ItemStackHandler {

        public Unsafe(int size) {
            super(size);
        }

        public void unsafeSetStackInSlot(int slot, @NotNull ItemStack stack) {
            validateSlotIndex(slot);
            this.stacks.set(slot, stack);
        }
    }

    public static class Reversed implements IItemHandlerModifiable {

        protected final IItemHandlerModifiable originalHandler;

        public Reversed(IItemHandlerModifiable orig) {
            originalHandler = orig;
        }

        private int getReversedSlot(int slot) {
            return getSlots() - 1 - slot;
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            originalHandler.setStackInSlot(getReversedSlot(slot), stack);
        }

        @Override
        public int getSlots() {
            return originalHandler.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return originalHandler.getStackInSlot(getReversedSlot(slot));
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return originalHandler.insertItem(getReversedSlot(slot), stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return originalHandler.extractItem(getReversedSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return originalHandler.getSlotLimit(getReversedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return originalHandler.isItemValid(getReversedSlot(slot), stack);
        }
    }
}
