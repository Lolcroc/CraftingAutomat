package lolcroc.craftingautomat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class CraftingAutomatScreen extends AbstractContainerScreen<CraftingAutomatMenu> {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 197;
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(CraftingAutomat.MODID, "textures/gui/container/crafting_automat.png");

    public CraftingAutomatScreen(final CraftingAutomatMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);

        imageWidth = WIDTH;
        imageHeight = HEIGHT;

        titleLabelX = 29;
    }

//    public void test() {
//        DynamicTexture texture = new DynamicTexture(256, 256, true);  // true for calloc
//
////        texture.setPixels(nativeimage);
//        texture.upload(); // If change something through setPixels
//
//        minecraft.getTextureManager().register(TEXTURE, texture); // autocloses
//    }

    private static final ResourceLocation CRAFTING_TABLE = new ResourceLocation("textures/gui/container/crafting_table.png");
    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final ResourceLocation FURNACE = new ResourceLocation("textures/gui/container/furnace.png");

    // Background
    @Override
    protected void renderBg(PoseStack stack, float partialTicks, int mouseX, int mouseY) {
        renderBackground(stack); // Do I need this?
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = leftPos;
        int j = topPos;
//        blit(stack, i, j, 0, 0, imageWidth, imageHeight);

        RenderSystem.setShaderTexture(0, CRAFTING_TABLE);
        blit(stack, i, j, 0, 0, imageWidth, 101);

        RenderSystem.setShaderTexture(0, CHEST);
        for (int k = 0; k < 13; k++) {
            blit(stack, i, j + 101 + k, 0, 138, imageWidth, 1);
        }
        blit(stack, i, j + 114, 0, 139, imageWidth, 83);

        // Draw progress bar
        RenderSystem.setShaderTexture(0, FURNACE);
//        blit(stack, i + 89, j + 34, 176, 0, menu.getProgressWidth() + 1, 16);
        blit(stack, i + 89, j + 34, 79, 34, 25, 16);
        blit(stack, i + 89, j + 34, 176, 14, menu.getProgressWidth() + 1, 16);

        // Draw crafting flag marker and tooltip
        CraftingAutomatBlockEntity.CraftingFlag flag = menu.getCraftingFlag();
        if (flag != CraftingAutomatBlockEntity.CraftingFlag.NONE) {
            RenderSystem.setShaderTexture(0, TEXTURE);
            blit(stack, i + 142, j + 26, 176 + 8 * (flag.getIndex() - 1), 17, 8, 8);

            if (isHovering(142, 26, 8, 8, mouseX, mouseY)) {
                renderTooltip(stack, flag.getDisplayTags(), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        super.render(stack, mouseX, mouseY, partialTicks);
        renderTooltip(stack, mouseX, mouseY); // Hovered tooltip
    }

}
