package lolcroc.craftingautomat.client;

import lolcroc.craftingautomat.CommonProxy;
import lolcroc.craftingautomat.CraftingAutomat;
import lolcroc.craftingautomat.block.BlockCraftingAutomat;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(modid = CraftingAutomat.MODID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {

	public static final String LOCATION = "lolcroc.craftingautomat.client.ClientProxy";
	
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		Item item = CraftingAutomat.Items.autocrafter;
		
		ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), "inventory");
		
		ModelLoader.setCustomModelResourceLocation(item, 0, mrl);
		ModelLoader.setCustomStateMapper(CraftingAutomat.Blocks.autocrafter, (new StateMap.Builder()).ignore(BlockCraftingAutomat.ACTIVE).build());
	}
}
