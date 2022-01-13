package lolcroc.craftingautomat;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
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
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @ObjectHolder(MODID)
    public static class Blocks {
        public static final Block autocrafter = null;
    }
    
    @ObjectHolder(MODID)
    public static class BlockEntityTypes {
        public static final BlockEntityType<CraftingAutomatBlockEntity> autocrafter = null;
    }
    
    @ObjectHolder(MODID)
    public static class MenuTypes {
        public static final MenuType<CraftingAutomatContainer> autocrafter = null;
    }
    
    public CraftingAutomat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CraftingAutomatConfig.COMMON_CONFIG);
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MenuScreens.register(CraftingAutomat.MenuTypes.autocrafter, CraftingAutomatScreen::new));
        CraftingAutomatNetwork.registerMessages();
    }
    
    @SubscribeEvent
    public static void registerMenus(RegistryEvent.Register<MenuType<?>> event) {
        event.getRegistry().register(IForgeMenuType.create(CraftingAutomatContainer::new).setRegistryName(CraftingAutomatBlock.REGISTRY_NAME));
    }
    
    @SubscribeEvent
    public static void registerTiles(RegistryEvent.Register<BlockEntityType<?>> event) {
        event.getRegistry().register(BlockEntityType.Builder.of(CraftingAutomatBlockEntity::new, CraftingAutomat.Blocks.autocrafter).build(null).setRegistryName(CraftingAutomatBlock.REGISTRY_NAME));
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(new CraftingAutomatBlock());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new BlockItem(CraftingAutomat.Blocks.autocrafter, new Item.Properties().tab(CreativeModeTab.TAB_REDSTONE)).setRegistryName(CraftingAutomat.Blocks.autocrafter.getRegistryName()));
    }
}
