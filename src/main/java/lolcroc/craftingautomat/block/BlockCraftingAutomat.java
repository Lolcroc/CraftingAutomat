package lolcroc.craftingautomat.block;

import lolcroc.craftingautomat.CraftingAutomat;
import lolcroc.craftingautomat.tileentity.TileEntityCraftingAutomat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.dispenser.PositionImpl;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class BlockCraftingAutomat extends BlockContainer {
	
	public static final String NAME = "autocrafter";
	
	public static final ResourceLocation DEFAULT = new ResourceLocation(CraftingAutomat.MODID, NAME);
	public static final DirectionProperty FACING = BlockHorizontal.HORIZONTAL_FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public BlockCraftingAutomat() {
		super(Block.Properties.create(Material.ROCK).hardnessAndResistance(3.5F));
		this.setRegistryName(DEFAULT);
        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, EnumFacing.NORTH).with(ACTIVE, Boolean.valueOf(false)));
	}
	
	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder) {
		builder.add(FACING, ACTIVE);
	}
	
	@Override
	public TileEntity createNewTileEntity(IBlockReader worldIn) {
		return new TileEntityCraftingAutomat();
	}
	
	@Override
	public IBlockState getStateForPlacement(BlockItemUseContext context) {
		return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite());
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		if (stack.hasDisplayName()) {
			TileEntity tileentity = worldIn.getTileEntity(pos);
			if (tileentity instanceof TileEntityCraftingAutomat) {
				((TileEntityCraftingAutomat)tileentity).setCustomName(stack.getDisplayName());
			}
		}

	}

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
    	return EnumBlockRenderType.MODEL;
    }
    
    @Override
    public IBlockState rotate(IBlockState state, Rotation rot) {
    	return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @SuppressWarnings("deprecation")
	@Override
    public IBlockState mirror(IBlockState state, Mirror mirrorIn) {
    	return state.rotate(mirrorIn.toRotation(state.get(FACING)));
    }

	@Override
	public boolean onBlockActivated(IBlockState state, World worldIn, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote) {
			return true;
		}
		else {
			TileEntity tileentity = worldIn.getTileEntity(pos);

			if (tileentity instanceof TileEntityCraftingAutomat) {
				NetworkHooks.openGui((EntityPlayerMP) player, (TileEntityCraftingAutomat) tileentity, pos);
				player.addStat(StatList.INTERACT_WITH_CRAFTING_TABLE);
			}

			return true;
		}
	}
	
	@Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
		if (tileentity instanceof TileEntityCraftingAutomat) {
        	TileEntityCraftingAutomat teca = (TileEntityCraftingAutomat) tileentity;
    		boolean pow = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(pos.up());
            boolean active = state.get(ACTIVE);
        	
        	if (!worldIn.isRemote && pow && !active && teca.hasRecipe()) {
        		worldIn.setBlockState(pos, state.with(ACTIVE, Boolean.valueOf(true)), 2);
            }
        }
    }
	
	@Override
	public void onReplaced(IBlockState state, World worldIn, BlockPos pos, IBlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			TileEntity tileentity = worldIn.getTileEntity(pos);

	        if (tileentity instanceof TileEntityCraftingAutomat) {
	        	TileEntityCraftingAutomat teca = (TileEntityCraftingAutomat) tileentity;
	            
	        	// Start from index 1 (excluding result slot)
	            for (int i = 1; i < teca.getSizeInventory(); ++i) {
	                ItemStack itemstack = teca.getStackInSlot(i);

	                if (!itemstack.isEmpty()) {
	                	InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), itemstack);
	                }
	            }
	            
	            worldIn.updateComparatorOutputLevel(pos, this);
	        }
	        
			super.onReplaced(state, worldIn, pos, newState, isMoving);
		}
	}
	
	public static IPosition getDispensePosition(IBlockSource coords) {
		EnumFacing enumfacing = coords.getBlockState().get(FACING);
		double d0 = coords.getX() + 0.7D * (double)enumfacing.getXOffset();
		double d1 = coords.getY() + 0.7D * (double)enumfacing.getYOffset();
		double d2 = coords.getZ() + 0.7D * (double)enumfacing.getZOffset();
		return new PositionImpl(d0, d1, d2);
	}

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
    	return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
    	TileEntity te = worldIn.getTileEntity(pos);
    	if (te instanceof TileEntityCraftingAutomat) {
    		int count = ((TileEntityCraftingAutomat) te).getRecipeCount();
    		return count != 0 ? (int) (Math.log(count) / Math.log(2)) + 1 : 0;
    	}
    	else {
    		return 0;
    	}
    }

}
