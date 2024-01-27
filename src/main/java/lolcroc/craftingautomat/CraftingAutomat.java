package lolcroc.craftingautomat;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CraftingAutomat.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CraftingAutomat
{
    public static final String MODID = "craftingautomat";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final String NAME = "autocrafter";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Block> AUTOCRAFTER_BLOCK = BLOCKS.register(NAME, CraftingAutomatBlock::new);
    public static final RegistryObject<BlockEntityType<CraftingAutomatBlockEntity>> AUTOCRAFTER_BLOCK_ENTITY = BLOCK_ENTITIES.register(NAME, () -> BlockEntityType.Builder.of(CraftingAutomatBlockEntity::new, AUTOCRAFTER_BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<CraftingAutomatContainer>> AUTOCRAFTER_MENU = MENUS.register(NAME, () -> IForgeMenuType.create(CraftingAutomatContainer::new));
    public static final RegistryObject<Item> AUTOCRAFTER_ITEM = ITEMS.register(NAME, () -> new BlockItem(AUTOCRAFTER_BLOCK.get(), new Item.Properties()));
    
    public CraftingAutomat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CraftingAutomatConfig.COMMON_CONFIG);
        MinecraftForge.EVENT_BUS.register(this);

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
        ITEMS.register(eventBus);

        eventBus.addListener(this::addAutomatToCreativeTab);
    }

    private void addAutomatToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS)
            event.accept(AUTOCRAFTER_ITEM::get);
    }
    
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(AUTOCRAFTER_MENU.get(), CraftingAutomatScreen::new)
        );
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(CraftingAutomatNetwork::registerMessages);
    }
}
