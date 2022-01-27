package lolcroc.craftingautomat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = CraftingAutomat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CraftingAutomatConfig {

    public static final String TICK_SETTINGS = "tickSettings";
    public static final String NETWORK_SETTINGS = "networkSettings";

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_CONFIG;

    public static final ForgeConfigSpec.IntValue CRAFTING_TICKS;
    public static final ForgeConfigSpec.IntValue COOLDOWN_TICKS;

    public static final ForgeConfigSpec.BooleanValue SYNC_TICK_SETTINGS;

    static {
        COMMON_BUILDER.comment("Tick settings (20 ticks = 1 second)").push(TICK_SETTINGS);
        CRAFTING_TICKS = COMMON_BUILDER.comment("Number of ticks to craft a recipe")
                .defineInRange("craftingTicks", 8, 1, Integer.MAX_VALUE);
        COOLDOWN_TICKS = COMMON_BUILDER.comment("Number of ticks to cool down")
                .defineInRange("cooldownTicks", 16, 1, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Network settings").push(NETWORK_SETTINGS);
        SYNC_TICK_SETTINGS = COMMON_BUILDER.comment("""
                Synchronize tick settings to players.
                Disabling this setting may cause visual glitches for other players in crafting progress.
                Other players may need to manually alter their tick settings to match the server host.""")
                .define("synchronizeTickSettings", true);
        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    // On client dist; send packet over logical server OR update locally logical client
    // On server dist; send packet over logical server
    public static void syncTickSettings(boolean logicalServer) {
        if (logicalServer) {
            if (SYNC_TICK_SETTINGS.get()) {
                CraftingAutomatNetwork.overrideClientConfigs(COOLDOWN_TICKS, CRAFTING_TICKS);
            }
            else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.putAll(Stream.of(COOLDOWN_TICKS, CRAFTING_TICKS)
                        .collect(Collectors.toMap(ForgeConfigSpec.ConfigValue::getPath, ForgeConfigSpec.ConfigValue::get))));
            }
        }
    }

    // Registered non-statically on the forge event bus
    // Check the logical side for sending a packet from server (which can be physical CLIENT/SERVER)
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncTickSettings(!event.getEntity().level.isClientSide);
    }

    // Fires on client and server dist
    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        syncTickSettings(ServerLifecycleHooks.getCurrentServer() != null);
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
