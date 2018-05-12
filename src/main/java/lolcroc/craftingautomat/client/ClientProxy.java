package lolcroc.craftingautomat.client;

import lolcroc.craftingautomat.CommonProxy;
import lolcroc.craftingautomat.CraftingAutomat;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@EventBusSubscriber(value = Side.CLIENT, modid = CraftingAutomat.MODID)
public class ClientProxy extends CommonProxy {

	public static final String LOCATION = "lolcroc.craftingautomat.client.ClientProxy";
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void registerModel(ModelRegistryEvent event) {
		Item item = CraftingAutomat.Items.autocrafter;
		
		ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), "inventory");
		
		ModelLoader.setCustomModelResourceLocation(item, 0, mrl);
	}
}
