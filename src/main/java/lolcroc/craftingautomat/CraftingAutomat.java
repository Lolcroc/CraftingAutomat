package lolcroc.craftingautomat;

import lolcroc.craftingautomat.block.BlockCraftingAutomat;
import lolcroc.craftingautomat.client.ClientProxy;
import lolcroc.craftingautomat.tileentity.TileEntityCraftingAutomat;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

@Mod(CraftingAutomat.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CraftingAutomat
{
    public static final String MODID = "craftingautomat";
    
    public static CraftingAutomat instance;
    public static CommonProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    
    @ObjectHolder(MODID)
    public static class Blocks {
    	public static final Block autocrafter = null;
    }
    
    @ObjectHolder(MODID)
    public static class Items {
    	public static final Item autocrafter = null;
    }
    
    @ObjectHolder(MODID)
    public static class TileEntityTypes {
    	public static final TileEntityType<TileEntityCraftingAutomat> autocrafter = null;
    }
    
    public CraftingAutomat() {
    	instance = this;
    }
    
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
    	proxy.setup(event);
    }
    
    @SubscribeEvent
    public static void registerTiles(RegistryEvent.Register<TileEntityType<?>> event) {
    	IForgeRegistry<TileEntityType<?>> registry = event.getRegistry();
    	
    	TileEntityType<TileEntityCraftingAutomat> tile = TileEntityType.Builder.create(TileEntityCraftingAutomat::new).build(null);
    	tile.setRegistryName(BlockCraftingAutomat.DEFAULT);
    	
    	registry.register(tile);
    }
	
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		IForgeRegistry<Block> registry = event.getRegistry();
		
		Block autoCrafter = new BlockCraftingAutomat();
		
		registry.register(autoCrafter);
	}
	
	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		IForgeRegistry<Item> registry = event.getRegistry();
		
		Item item = new ItemBlock(CraftingAutomat.Blocks.autocrafter, new Item.Properties().group(ItemGroup.REDSTONE)).setRegistryName(CraftingAutomat.Blocks.autocrafter.getRegistryName());
		
		registry.register(item);
	}
}
