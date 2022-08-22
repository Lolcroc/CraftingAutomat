package lolcroc.craftingautomat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.PositionImpl;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.Iterator;

public class ResultHandler extends ItemStackHandler {

    private static final Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

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
                    e.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, outputface.getOpposite()).isPresent()
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
}