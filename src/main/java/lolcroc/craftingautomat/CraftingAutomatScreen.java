package lolcroc.craftingautomat;

import com.mojang.blaze3d.matrix.MatrixStack;
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

        field_238742_p_ = 28; // Title x
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

    // Background
    @Override
    protected void func_230450_a_(MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
        func_230446_a_(stack);
        field_230706_i_.getTextureManager().bindTexture(TEXTURE);
        int i = this.guiLeft;
        int j = this.guiTop;
        func_238474_b_(stack, i, j, 0, 0, xSize, ySize); // blit
        
        int w = getProgressWidth(24);
        func_238474_b_(stack, i + 89, j + 34, 176, 0, w + 1, 16);

        // Draw crafting flag marker and tooltip
        CraftingAutomatTileEntity.CraftingFlag flag = container.getCraftingFlag();
        if (flag != CraftingAutomatTileEntity.CraftingFlag.NONE) {
            func_238474_b_(stack, i + 142, j + 26, 176 + 8 * (flag.getIndex() - 1), 17, 8, 8);

            if (isPointInRegion(142, 26, 8, 8, mouseX, mouseY)) {
                func_243308_b(stack, flag.getDisplayTags(), mouseX, mouseY); // Tooltip
            }
        }
    }

    // Render
    @Override
    public void func_230430_a_(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        super.func_230430_a_(stack, mouseX, mouseY, partialTicks);
        func_230459_a_(stack, mouseX, mouseY); // Hovered tooltip
    }

}
