package lolcroc.craftingautomat;

import net.minecraftforge.common.ForgeConfigSpec;

public class CraftingAutomatConfig {

    public static final String TICK_SETTINGS = "tickSettings";

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec COMMON_CONFIG;

    public static ForgeConfigSpec.IntValue CRAFTING_TICKS;
    public static ForgeConfigSpec.IntValue COOLDOWN_TICKS;

    static {
        COMMON_BUILDER.comment("Tick settings (20 ticks = 1 second)").push(TICK_SETTINGS);
        CRAFTING_TICKS = COMMON_BUILDER.comment("Number of ticks to craft a recipe")
                .defineInRange("craftingTicks", 8, 1, Integer.MAX_VALUE);
        COOLDOWN_TICKS = COMMON_BUILDER.comment("Number of ticks to cool down")
                .defineInRange("cooldownTicks", 16, 1, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
    }

}
