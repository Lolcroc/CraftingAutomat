package lolcroc.craftingautomat;

import com.google.common.collect.Sets;
import lolcroc.craftingautomat.mixin.RecipeBookAccessor;
import net.minecraft.ResourceLocationException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

@Mod.EventBusSubscriber(modid=CraftingAutomat.MODID)
public class RecipesSavedData extends SavedData {

    protected final Set<ResourceLocation> recipes = Sets.newHashSet();

    public static RecipesSavedData load(CompoundTag tag, ServerLevel level) {
        RecipesSavedData data = new RecipesSavedData();
        RecipeManager manager = level.getRecipeManager();

        ListTag listtag = tag.getList("recipes", 8);
        for (int i = 0; i < listtag.size(); ++i) {
            String s = listtag.getString(i);

            try {
                ResourceLocation resourcelocation = new ResourceLocation(s);
                Optional<? extends Recipe<?>> optional = manager.byKey(resourcelocation);

                if (optional.isEmpty()) {
                    CraftingAutomat.LOGGER.error("Crafting Automat tried to load unrecognized recipe: {} removed now.", resourcelocation);
                }
                else {
                    data.recipes.add(optional.get().getId());
                }
            }
            catch (ResourceLocationException exception) {
                CraftingAutomat.LOGGER.error("Crafting Automat tried to load improperly formatted recipe: {} removed now.", s);
            }
        }

        return data;
    }

    public static RecipesSavedData computeIfAbsent(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                tag -> RecipesSavedData.load(tag, overworld),
                RecipesSavedData::new,
                "globalRecipeBook"
        );
    }

    public void updateRecipes(RecipeBook from) {
        this.recipes.addAll(((RecipeBookAccessor) from).getKnown());
    }

    public boolean contains(@Nullable Recipe<?> recipe) {
        return recipe != null && this.recipes.contains(recipe.getId());
    }

    @Nonnull
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag listtag = new ListTag();

        for(ResourceLocation resourcelocation : recipes) {
            listtag.add(StringTag.valueOf(resourcelocation.toString()));
        }

        tag.put("recipes", listtag);
        return tag;
    }

    @SubscribeEvent
    public static void onLoadLevel(LevelEvent.Load event) {
        LevelAccessor accessor = event.getLevel();
        if (accessor instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            computeIfAbsent(level.getServer());
        }
    }

}
