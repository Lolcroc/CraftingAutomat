package lolcroc.craftingautomat;

import lolcroc.craftingautomat.client.ClientProxy;
import lolcroc.craftingautomat.handler.GuiHandler;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

@Mod(modid = CraftingAutomat.MODID, name = CraftingAutomat.NAME, version = CraftingAutomat.VERSION)
public class CraftingAutomat
{
    public static final String MODID = "craftingautomat";
    public static final String NAME = "Crafting Automat";
    public static final String VERSION = "1.0.5";
    
    @ObjectHolder(MODID)
    public static class Blocks {
    	public static final Block autocrafter = null;
    }
    
    @ObjectHolder(MODID)
    public static class Items {
    	public static final Item autocrafter = null;
    }
    
    @SidedProxy(clientSide = ClientProxy.LOCATION, serverSide = CommonProxy.LOCATION)
    public static CommonProxy proxy;
    
    @Instance
    public static CraftingAutomat instance;

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
    }
}
