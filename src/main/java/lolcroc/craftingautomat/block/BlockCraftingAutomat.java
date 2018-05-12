package lolcroc.craftingautomat.block;

import java.util.Random;

import lolcroc.craftingautomat.CraftingAutomat;
import lolcroc.craftingautomat.tileentity.TileEntityCraftingAutomat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.dispenser.PositionImpl;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCraftingAutomat extends BlockContainer {
	
	public static final String NAME = "autocrafter";
	
	public static final ResourceLocation DEFAULT = new ResourceLocation(CraftingAutomat.MODID, NAME);
	public static final PropertyDirection FACING = BlockHorizontal.FACING;

	public BlockCraftingAutomat() {
		super(Material.ROCK);
		this.setUnlocalizedName(DEFAULT.toString());
		this.setRegistryName(DEFAULT);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.setCreativeTab(CreativeTabs.REDSTONE);
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		super.onBlockAdded(worldIn, pos, state);
		this.setDefaultFacing(worldIn, pos, state);
	}

	@Override
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}
	
	private void setDefaultFacing(World worldIn, BlockPos pos, IBlockState state)
	{
		if (!worldIn.isRemote)
		{
			IBlockState iblockstate = worldIn.getBlockState(pos.north());
			IBlockState iblockstate1 = worldIn.getBlockState(pos.south());
			IBlockState iblockstate2 = worldIn.getBlockState(pos.west());
			IBlockState iblockstate3 = worldIn.getBlockState(pos.east());
			EnumFacing enumfacing = (EnumFacing)state.getValue(FACING);

			if (enumfacing == EnumFacing.NORTH && iblockstate.isFullBlock() && !iblockstate1.isFullBlock())
			{
				enumfacing = EnumFacing.SOUTH;
			}
			else if (enumfacing == EnumFacing.SOUTH && iblockstate1.isFullBlock() && !iblockstate.isFullBlock())
			{
				enumfacing = EnumFacing.NORTH;
			}
			else if (enumfacing == EnumFacing.WEST && iblockstate2.isFullBlock() && !iblockstate3.isFullBlock())
			{
				enumfacing = EnumFacing.EAST;
			}
			else if (enumfacing == EnumFacing.EAST && iblockstate3.isFullBlock() && !iblockstate2.isFullBlock())
			{
				enumfacing = EnumFacing.WEST;
			}

			worldIn.setBlockState(pos, state.withProperty(FACING, enumfacing), 2);
		}
	}
	
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);

        if (stack.hasDisplayName())
        {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityCraftingAutomat)
            {
                ((TileEntityCraftingAutomat)tileentity).setCustomName(stack.getDisplayName());
            }
        }
    }

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityCraftingAutomat();
	}
	
	@Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Item.getItemFromBlock(CraftingAutomat.Blocks.autocrafter);
    }
	
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
	
    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing enumfacing = EnumFacing.getFront(meta);

        if (enumfacing.getAxis() == EnumFacing.Axis.Y)
        {
            enumfacing = EnumFacing.NORTH;
        }

        return this.getDefaultState().withProperty(FACING, enumfacing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return ((EnumFacing)state.getValue(FACING)).getIndex();
    }
	
    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation((EnumFacing)state.getValue(FACING)));
    }
	
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] {FACING});
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
	{
		if (worldIn.isRemote) {
			return true;
		}
		else {
			TileEntity tileentity = worldIn.getTileEntity(pos);

			if (tileentity instanceof TileEntityCraftingAutomat) {
				playerIn.openGui(CraftingAutomat.instance, 0, worldIn, pos.getX(), pos.getY(), pos.getZ());
				playerIn.addStat(StatList.CRAFTING_TABLE_INTERACTION);
			}

			return true;
		}
	}
	
	@Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        boolean pow = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(pos.up());
        TileEntity tileentity = worldIn.getTileEntity(pos);
        
        if (tileentity instanceof TileEntityCraftingAutomat) {
        	TileEntityCraftingAutomat teca = (TileEntityCraftingAutomat) tileentity;
        	
        	if (!worldIn.isRemote && pow && teca.canCraft()) {
                teca.initiateCrafting();
            }
        }
    }
	
	@Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
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

        super.breakBlock(worldIn, pos, state);
    }
	
    public static IPosition getDispensePosition(IBlockSource coords) {
        EnumFacing enumfacing = (EnumFacing)coords.getBlockState().getValue(FACING);
        double d0 = coords.getX() + 0.7D * (double)enumfacing.getFrontOffsetX();
        double d1 = coords.getY() + 0.7D * (double)enumfacing.getFrontOffsetY();
        double d2 = coords.getZ() + 0.7D * (double)enumfacing.getFrontOffsetZ();
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
