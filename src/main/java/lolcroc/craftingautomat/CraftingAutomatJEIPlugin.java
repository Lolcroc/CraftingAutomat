package lolcroc.craftingautomat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.util.ResourceLocation;

@JeiPlugin
public class CraftingAutomatJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return CraftingAutomatBlock.REGISTRY_NAME;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(CraftingAutomatContainer.class, VanillaRecipeCategoryUid.CRAFTING, 1, 9, 10, 45);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CraftingAutomatScreen.class, 88, 32, 28, 23, VanillaRecipeCategoryUid.CRAFTING);
    }
}
