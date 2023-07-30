package lolcroc.craftingautomat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class CraftingAutomatScreen extends AbstractContainerScreen<CraftingAutomatContainer> {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 197;
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(CraftingAutomat.MODID, "textures/gui/container/crafting_automat.png");

    private static final Supplier<Integer> CRAFTING_TICKS = () -> CraftingAutomatConfig.Client.get(CraftingAutomatConfig.CRAFTING_TICKS);
    private static final Supplier<Integer> COOLDOWN_TICKS = () -> CraftingAutomatConfig.Client.get(CraftingAutomatConfig.COOLDOWN_TICKS);

    public CraftingAutomatScreen(final CraftingAutomatContainer container, Inventory inventory, Component title) {
        super(container, inventory, title);

        imageWidth = WIDTH;
        imageHeight = HEIGHT;

        titleLabelX = 28;
    }

    private int getProgressWidth() {
        int ticks = menu.getProgress();

        if (ticks <= 0) {
            return 0; // Easy return
        }
        else if (ticks > CRAFTING_TICKS.get()) {
            return (COOLDOWN_TICKS.get() + CRAFTING_TICKS.get() - ticks) * 24 / COOLDOWN_TICKS.get();
        }
        else {
            return ticks * 24 / CRAFTING_TICKS.get();
        }
    }

    // Background
    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        renderBackground(gfx); // Do I need this?

        int i = leftPos;
        int j = topPos;
        gfx.blit(TEXTURE, i, j, 0, 0, imageWidth, imageHeight);
        
        int w = getProgressWidth();
        gfx.blit(TEXTURE, i + 89, j + 34, 176, 0, w + 1, 16);

        // Draw crafting flag marker and tooltip
        CraftingAutomatBlockEntity.CraftingFlag flag = menu.getCraftingFlag();
        if (flag != CraftingAutomatBlockEntity.CraftingFlag.NONE) {
            gfx.blit(TEXTURE, i + 142, j + 26, 176 + 8 * (flag.getIndex() - 1), 17, 8, 8);

            if (isHovering(142, 26, 8, 8, mouseX, mouseY)) {
                gfx.renderTooltip(font, flag.getDisplayTags(), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        super.render(gfx, mouseX, mouseY, partialTicks);
        renderTooltip(gfx, mouseX, mouseY); // Hovered tooltip
    }

}
