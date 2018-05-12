package lolcroc.craftingautomat;

import lolcroc.craftingautomat.block.BlockCraftingAutomat;
import lolcroc.craftingautomat.tileentity.TileEntityCraftingAutomat;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

@EventBusSubscriber(modid = CraftingAutomat.MODID)
public class CommonProxy {
	
	public static final String LOCATION = "lolcroc.craftingautomat.CommonProxy";
	
	static {
		GameRegistry.registerTileEntity(TileEntityCraftingAutomat.class, BlockCraftingAutomat.DEFAULT.toString());
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
		
		Item item = new ItemBlock(CraftingAutomat.Blocks.autocrafter).setRegistryName(CraftingAutomat.Blocks.autocrafter.getRegistryName());
		
		registry.register(item);
	}

}
