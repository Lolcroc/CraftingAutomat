package lolcroc.craftingautomat;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CraftingAutomatScreen extends ContainerScreen<CraftingAutomatContainer> {
	
    public static final int WIDTH = 176;
    public static final int HEIGHT = 197;

    private static final int CRAFTING_TICKS = CraftingAutomatConfig.CRAFTING_TICKS.get();
    private static final int COOLDOWN_TICKS = CraftingAutomatConfig.COOLDOWN_TICKS.get();
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(CraftingAutomat.MODID, "textures/gui/container/crafting_automat.png");
    
	public CraftingAutomatScreen(final CraftingAutomatContainer container, PlayerInventory inventory, ITextComponent title) {
		super(container, inventory, title);
		
		this.xSize = WIDTH;
		this.ySize = HEIGHT;
	}
	
    private int getProgressWidth(int width) {
        int ticks = this.container.getProgress();
        if (ticks <= 0) {
        	return 0; // Easy return
        }
        else if (ticks > CRAFTING_TICKS) {
        	return (COOLDOWN_TICKS + CRAFTING_TICKS - ticks) * width / COOLDOWN_TICKS;
        }
        else {
        	return ticks * width / CRAFTING_TICKS;
        }
    }
	
	@Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        this.font.drawString(this.title.getFormattedText(), 28, 6, 4210752);
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 4210752);
    }
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bindTexture(TEXTURE);
		int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.blit(i, j, 0, 0, this.xSize, this.ySize);
        
        int w = this.getProgressWidth(24);
        this.blit(i + 89, j + 34, 176, 0, w + 1, 16);

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
		this.renderBackground();
		super.render(mouseX, mouseY, partialTicks);
		this.renderHoveredToolTip(mouseX, mouseY);
	}

}
