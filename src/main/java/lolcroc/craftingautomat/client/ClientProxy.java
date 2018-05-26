package lolcroc.craftingautomat.client;

import lolcroc.craftingautomat.CommonProxy;
import lolcroc.craftingautomat.CraftingAutomat;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber(modid = CraftingAutomat.MODID)
public class ClientProxy extends CommonProxy {

	public static final String LOCATION = "lolcroc.craftingautomat.client.ClientProxy";
	
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		Item item = CraftingAutomat.Items.autocrafter;
		
		ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), "inventory");
		
		ModelLoader.setCustomModelResourceLocation(item, 0, mrl);
	}
}
