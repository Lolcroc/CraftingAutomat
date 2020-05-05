package lolcroc.craftingautomat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CraftingAutomatBlock extends Block {

    public static final String NAME = "autocrafter";

    // Local vars are fast
    @CapabilityInject(IItemHandler.class)
    private static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(CraftingAutomat.MODID, NAME);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public CraftingAutomatBlock() {
        super(Properties.create(Material.ROCK).sound(SoundType.STONE).hardnessAndResistance(3.5F));
        this.setRegistryName(REGISTRY_NAME);
        this.setDefaultState(this.stateContainer.getBaseState()
                .with(FACING, Direction.NORTH)
                .with(ACTIVE, Boolean.FALSE)
                .with(TRIGGERED, Boolean.FALSE));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, TRIGGERED);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader worldIn) {
        return new CraftingAutomatTileEntity();
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (stack.hasDisplayName()) {
            safeConsume(worldIn, pos, t -> t.setCustomName(stack.getDisplayName()));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote) {
            safeConsume(worldIn, pos, t -> {
                NetworkHooks.openGui((ServerPlayerEntity) player, t, pos);
                player.addStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
            });
        }
        return ActionResultType.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        safeConsume(world, pos, t -> {
            boolean pow = world.isBlockPowered(pos) || world.isBlockPowered(pos.up());
            boolean triggered = state.get(TRIGGERED);
            if (pow && !triggered) {
                BlockState newstate = state.with(TRIGGERED, Boolean.TRUE);
                // Below statement takes the role of scheduleTick in DispenserBlock
                if (!state.get(ACTIVE)) newstate = newstate.with(ACTIVE, t.isReady());
                world.setBlockState(pos, newstate, 4);
            }
            else if (!pow && triggered) {
                world.setBlockState(pos, state.with(TRIGGERED, Boolean.FALSE), 4);
            }
        });

        super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            safeConsume(world, pos, t -> {
                t.getCapability(ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                    for (int i = 0; i < h.getSlots(); i++) {
                        InventoryHelper.spawnItemStack(world, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), h.getStackInSlot(i));
                    }
                });
            });
            world.updateComparatorOutputLevel(pos, this);

            super.onReplaced(state, world, pos, newState, isMoving);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getComparatorInputOverride(BlockState blockState, World worldIn, BlockPos pos) {
        return safeFunc(worldIn, pos, t -> {
            int count = t.getRecipeCount();
            return count != 0 ? (int) (Math.log(count) / Math.log(2)) + 1 : 0;
        }, () -> 0);
    }

    // Prevents incoming redstone signals to propagate to adjacent blocks
    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side) {
        return false;
    }

    private static void safeConsume(World world, BlockPos pos, Consumer<CraftingAutomatTileEntity> c) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof CraftingAutomatTileEntity) {
            c.accept((CraftingAutomatTileEntity) tile);
        }
    }

    private static <T> T safeFunc(World world, BlockPos pos, Function<CraftingAutomatTileEntity, T> f, Supplier<T> other) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof CraftingAutomatTileEntity ? f.apply((CraftingAutomatTileEntity) tile) : other.get();
    }
}
