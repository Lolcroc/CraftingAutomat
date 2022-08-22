package lolcroc.craftingautomat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CraftingAutomatBlock extends BaseEntityBlock {

    public static final String NAME = "autocrafter";

    // Local vars are fast
    private static final Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(CraftingAutomat.MODID, NAME);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public CraftingAutomatBlock() {
        // RedstoneConductor : Prevents incoming redstone signals to propagate to adjacent blocks
        super(BlockBehaviour.Properties.of(Material.STONE).sound(SoundType.STONE)
                .strength(3.5F).isRedstoneConductor((state, getter, pos) -> false));
        this.registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, Boolean.FALSE)
                .setValue(TRIGGERED, Boolean.FALSE));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, TRIGGERED);
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingAutomatBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !state.getValue(ACTIVE) ? null : createTickerHelper(type, CraftingAutomat.AUTOCRAFTER_BLOCK_ENTITY.get(), CraftingAutomatBlockEntity::serverTick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }


    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            safeConsume(level, pos, t -> t.setCustomName(stack.getDisplayName()));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            safeConsume(level, pos, t -> {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                RecipesSavedData.computeIfAbsent(level.getServer()).updateRecipes(serverPlayer.getRecipeBook());
                NetworkHooks.openScreen(serverPlayer, t, pos);
                player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
            });
        }
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        safeConsume(level, pos, t -> {
            boolean pow = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
            boolean triggered = state.getValue(TRIGGERED);
            if (pow && !triggered) {
                BlockState newstate = state.setValue(TRIGGERED, Boolean.TRUE);
                // Below statement takes the role of scheduleTick in DispenserBlock
                if (!state.getValue(ACTIVE)) newstate = newstate.setValue(ACTIVE, t.isReady());
                level.setBlock(pos, newstate, 4);
            }
            else if (!pow && triggered) {
                level.setBlock(pos, state.setValue(TRIGGERED, Boolean.FALSE), 4);
            }
        });

        super.neighborChanged(state, level, pos, blockIn, fromPos, isMoving);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            safeConsume(level, pos, t -> {
                t.getCapability(ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                    for (int i = 0; i < h.getSlots(); i++) {
                        Containers.dropItemStack(level, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), h.getStackInSlot(i));
                    }
                });
            });
            level.updateNeighbourForOutputSignal(pos, this);

            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        return safeFunc(level, pos, t -> {
            int count = t.getRecipeCount();
            return count != 0 ? (int) (Math.log(count) / Math.log(2)) + 1 : 0;
        }, () -> 0);
    }

    private static void safeConsume(Level level, BlockPos pos, Consumer<CraftingAutomatBlockEntity> c) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof CraftingAutomatBlockEntity) {
            c.accept((CraftingAutomatBlockEntity) tile);
        }
    }

    private static <T> T safeFunc(Level level, BlockPos pos, Function<CraftingAutomatBlockEntity, T> f, Supplier<T> other) {
        BlockEntity tile = level.getBlockEntity(pos);
        return tile instanceof CraftingAutomatBlockEntity ? f.apply((CraftingAutomatBlockEntity) tile) : other.get();
    }
}
