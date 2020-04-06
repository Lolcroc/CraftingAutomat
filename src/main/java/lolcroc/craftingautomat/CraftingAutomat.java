package lolcroc.craftingautomat;

import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CraftingAutomat.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CraftingAutomat
{
    public static final String MODID = "craftingautomat";
//    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    public static CraftingAutomat instance;

    @ObjectHolder(MODID)
    public static class Blocks {
    	public static final Block autocrafter = null;
    }
    
    @ObjectHolder(MODID)
    public static class TileEntityTypes {
    	public static final TileEntityType<CraftingAutomatTileEntity> autocrafter = null;
    }
    
    @ObjectHolder(MODID)
    public static class ContainerTypes {
    	public static final ContainerType<CraftingAutomatContainer> autocrafter = null;
    }
    
    public CraftingAutomat() {
    	instance = this;
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CraftingAutomatConfig.COMMON_CONFIG);
    }
    
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ScreenManager.registerFactory(CraftingAutomat.ContainerTypes.autocrafter, CraftingAutomatScreen::new));
    }
    
    @SubscribeEvent
    public static void registerContainers(RegistryEvent.Register<ContainerType<?>> event) {
        event.getRegistry().register(IForgeContainerType.create(CraftingAutomatContainer::new).setRegistryName(CraftingAutomatBlock.REGISTRY_NAME));
    }
    
    @SubscribeEvent
    public static void registerTiles(RegistryEvent.Register<TileEntityType<?>> event) {
        event.getRegistry().register(TileEntityType.Builder.create(CraftingAutomatTileEntity::new, CraftingAutomat.Blocks.autocrafter).build(null).setRegistryName(CraftingAutomatBlock.REGISTRY_NAME));
    }
	
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(new CraftingAutomatBlock());
	}
	
	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new BlockItem(CraftingAutomat.Blocks.autocrafter, new Item.Properties().group(ItemGroup.REDSTONE)).setRegistryName(CraftingAutomat.Blocks.autocrafter.getRegistryName()));
	}
}
