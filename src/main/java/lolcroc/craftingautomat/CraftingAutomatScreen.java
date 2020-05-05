package lolcroc.craftingautomat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class CraftingAutomatScreen extends ContainerScreen<CraftingAutomatContainer> {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 197;
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(CraftingAutomat.MODID, "textures/gui/container/crafting_automat.png");

    private static final Supplier<Integer> CRAFTING_TICKS = () -> CraftingAutomatConfig.Client.get(CraftingAutomatConfig.CRAFTING_TICKS);
    private static final Supplier<Integer> COOLDOWN_TICKS = () -> CraftingAutomatConfig.Client.get(CraftingAutomatConfig.COOLDOWN_TICKS);

    public CraftingAutomatScreen(final CraftingAutomatContainer container, PlayerInventory inventory, ITextComponent title) {
        super(container, inventory, title);

        xSize = WIDTH;
        ySize = HEIGHT;
    }

    private int getProgressWidth(int width) {
        int ticks = container.getProgress();

        if (ticks <= 0) {
            return 0; // Easy return
        }
        else if (ticks > CRAFTING_TICKS.get()) {
            return (COOLDOWN_TICKS.get() + CRAFTING_TICKS.get() - ticks) * width / COOLDOWN_TICKS.get();
        }
        else {
            return ticks * width / CRAFTING_TICKS.get();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        font.drawString(title.getFormattedText(), 28, 6, 4210752);
        font.drawString(playerInventory.getDisplayName().getFormattedText(), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        minecraft.getTextureManager().bindTexture(TEXTURE);
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        blit(i, j, 0, 0, xSize, ySize);
        
        int w = getProgressWidth(24);
        blit(i + 89, j + 34, 176, 0, w + 1, 16);

        // Draw crafting flag marker and tooltip
        CraftingAutomatTileEntity.CraftingFlag flag = container.getCraftingFlag();
        if (flag != CraftingAutomatTileEntity.CraftingFlag.NONE) {
            blit(i + 142, j + 26, 176 + 8 * (flag.getIndex() - 1), 17, 8, 8);

            if (isPointInRegion(142, 26, 8, 8, mouseX, mouseY)) {
                renderTooltip(I18n.format(flag.getDisplayName().getFormattedText()), mouseX, mouseY);
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

}
