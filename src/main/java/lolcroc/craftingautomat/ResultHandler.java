package lolcroc.craftingautomat;

import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.Position;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.minecart.ContainerMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.Iterator;

public class ResultHandler extends ItemStackHandler {

    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;

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

    protected static void dispense(World world, BlockPos pos, Direction side, ItemStack stack, boolean silent) {
        double x = pos.getX() + 0.5D + 0.7D * (double)side.getXOffset();
        double y = pos.getY() + 0.5D + 0.7D * (double)side.getYOffset();
        double z = pos.getZ() + 0.5D + 0.7D * (double)side.getZOffset();

        // Lower the dispense position slightly when shooting from the side
        if (side.getAxis().isHorizontal()) {
            y -= 0.2D;
        }

        ItemStack itemstack = stack.split(stack.getCount()); //is empty afterwards
        DefaultDispenseItemBehavior.doDispense(world, itemstack, 6, side, new Position(x, y, z));

        if (!silent) {
            world.playEvent(1000, pos, 0); // Play dispense sound
            world.playEvent(2000, pos, side.getIndex()); // Spawn dispense particles
        }
    }

    private static ItemStack insertStack(ICapabilityProvider provider, Direction input, ItemStack stack) {
        return provider.getCapability(ITEM_HANDLER_CAPABILITY, input)
                .map(h -> ItemHandlerHelper.insertItemStacked(h, stack, false))
                .orElse(stack);
    }

    public static void outputStack(TileEntity tile, ItemStack stack, boolean silent) {
        BlockPos tilepos = tile.getPos();
        World world = tile.getWorld();
        Direction outputface = tile.getBlockState().get(CraftingAutomatBlock.FACING);
        BlockPos targetpos = tilepos.offset(outputface);

        if (world.getBlockState(targetpos).hasTileEntity()) {
            TileEntity tileentity = world.getTileEntity(targetpos);
            if (tileentity != null) {
                stack = insertStack(tileentity, outputface.getOpposite(), stack);
            }
        }
        else {
            // Only allow hopper/chest minecarts for now
            Iterator<Entity> invents = world.getEntitiesInAABBexcluding(null, new AxisAlignedBB(targetpos), e ->
                    e.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, outputface.getOpposite()).isPresent()
                            && e.isAlive() && (e instanceof ContainerMinecartEntity)).iterator();

            while (invents.hasNext() && !stack.isEmpty()) {
                stack = insertStack(invents.next(), outputface.getOpposite(), stack);
            }
        }

        // Dispense the remainder
        if (!stack.isEmpty()) {
            dispense(world, tilepos, outputface, stack, silent);
        }
    }
}