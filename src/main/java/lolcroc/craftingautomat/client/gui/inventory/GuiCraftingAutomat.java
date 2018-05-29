package lolcroc.craftingautomat.client.gui.inventory;

import lolcroc.craftingautomat.CraftingAutomat;
import lolcroc.craftingautomat.inventory.ContainerCraftingAutomat;
import lolcroc.craftingautomat.tileentity.TileEntityCraftingAutomat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiCraftingAutomat extends GuiContainer {
	
    public static final int WIDTH = 176;
    public static final int HEIGHT = 197;
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(CraftingAutomat.MODID, "textures/gui/container/crafting_automat.png");

    private final TileEntityCraftingAutomat inventory;
    private final InventoryPlayer playerInventory;
    
	public GuiCraftingAutomat(InventoryPlayer playerInv, TileEntityCraftingAutomat inv) {
		super(new ContainerCraftingAutomat(playerInv, inv));
		
		this.inventory = inv;
		this.playerInventory = playerInv;
		this.xSize = WIDTH;
		this.ySize = HEIGHT;
	}
	
    private int getProgressWidth(int width) {
        int ticks = this.inventory.getField(0);
        if (ticks == 0) {
        	return 0; // Easy return
        }
        else if (ticks > TileEntityCraftingAutomat.CRAFTING_TICKS) {
        	return (TileEntityCraftingAutomat.COOLDOWN_TICKS + TileEntityCraftingAutomat.CRAFTING_TICKS - ticks) * width / TileEntityCraftingAutomat.COOLDOWN_TICKS;
        }
        else {
        	return ticks * width / TileEntityCraftingAutomat.CRAFTING_TICKS;
        }
    }
	
	@Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        this.fontRenderer.drawString(this.inventory.getDisplayName().getUnformattedText(), 28, 6, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
    }
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(TEXTURE);
		int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
        
        int w = this.getProgressWidth(24);
        this.drawTexturedModalRect(i + 89, j + 34, 176, 0, w + 1, 16);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		this.renderHoveredToolTip(mouseX, mouseY);
	}

}
