package lolcroc.craftingautomat.client;

import lolcroc.craftingautomat.CommonProxy;
import lolcroc.craftingautomat.CraftingAutomat;
import lolcroc.craftingautomat.client.gui.inventory.GuiCraftingAutomat;
import lolcroc.craftingautomat.tileentity.TileEntityCraftingAutomat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.FMLPlayMessages;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {
	
	@Override
	public void setup(FMLCommonSetupEvent event) {
		OBJLoader.INSTANCE.addDomain(CraftingAutomat.MODID);
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.GUIFACTORY, () -> ClientProxy::openGui);
	}
		
	public static GuiScreen openGui(FMLPlayMessages.OpenContainer openContainer) {
		BlockPos pos = openContainer.getAdditionalData().readBlockPos();
		EntityPlayerSP player = Minecraft.getInstance().player;
		TileEntity te = Minecraft.getInstance().world.getTileEntity(pos);

		if (te instanceof TileEntityCraftingAutomat) {
			return new GuiCraftingAutomat(player.inventory, (TileEntityCraftingAutomat) te);
		}

		return null;
	}
}
