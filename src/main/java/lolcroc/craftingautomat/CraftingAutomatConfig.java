package lolcroc.craftingautomat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CraftingAutomat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CraftingAutomatConfig {

    public static final String TICK_SETTINGS = "tickSettings";

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_CONFIG;

    public static final ForgeConfigSpec.IntValue CRAFTING_TICKS;
    public static final ForgeConfigSpec.IntValue COOLDOWN_TICKS;

    static {
        COMMON_BUILDER.comment("Tick settings (20 ticks = 1 second)").push(TICK_SETTINGS);
        CRAFTING_TICKS = COMMON_BUILDER.comment("Number of ticks to craft a recipe")
                .defineInRange("craftingTicks", 8, 1, Integer.MAX_VALUE);
        COOLDOWN_TICKS = COMMON_BUILDER.comment("Number of ticks to cool down")
                .defineInRange("cooldownTicks", 16, 1, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    // Fires on client and server dist
    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            CraftingAutomatNetwork.overrideClientConfigs(COOLDOWN_TICKS, CRAFTING_TICKS);
        }
    }

    // A silly way to respect Dedicated Server config values without needing to update the Client ones
    // This way players can keep their own settings but still play on servers with different ones
    @OnlyIn(Dist.CLIENT)
    public static class Client {
        private static final Map<List<String>, Integer> VALUES = new HashMap<>();

        public static Integer get(ForgeConfigSpec.IntValue cfgval) {
            return VALUES.getOrDefault(cfgval.getPath(), cfgval.get());
        }

        public static void putAll(Map<List<String>, Integer> vals) {
            VALUES.putAll(vals);
        }
    }
 }
