package lolcroc.craftingautomat;

import com.google.common.collect.Lists;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CraftingAutomatResultSlot extends SlotItemHandler {

    private final CraftingAutomatBlockEntity automatEntity;
    private final Player player;
    private int amountCrafted;

    public CraftingAutomatResultSlot(IItemHandler handler, Player player, CraftingAutomatBlockEntity te, int slotIndex, int xPosition, int yPosition) {
        super(handler, slotIndex, xPosition, yPosition);
        this.automatEntity = te;
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack stack = getItem();
        amountCrafted += stack.getCount(); // Wow vanilla actually does this wrong
        automatEntity.result.setStackInSlot(0, ItemStack.EMPTY);
        return stack;
    }

    // Overrides extract method
    @Override
    public boolean mayPickup(Player playerIn) {
        return true;
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        amountCrafted += amount;
        checkTakeAchievements(stack);
    }

    @Override
    protected void onSwapCraft(int p_190900_1_) {
        amountCrafted += p_190900_1_;
    }

    @Override
    public void onQuickCraft(ItemStack stack, ItemStack other) {
        int i = other.getCount() - stack.getCount();
        if (i > 0) {
            onQuickCraft(other, i);
        }
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        //Fire Item.onCrafting and Forge crafting hook
        if (amountCrafted > 0) {
            stack.onCraftedBy(player.level, player, amountCrafted);
            ForgeEventFactory.firePlayerCraftingEvent(player, stack, automatEntity.matrixWrapper);
        }
        amountCrafted = 0;
        
        //Unlock recipe
        Recipe<?> recipe = automatEntity.getRecipeUsed();
        if (recipe != null && !recipe.isSpecial()) {
            player.awardRecipes(Lists.newArrayList(recipe));
        }
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        checkTakeAchievements(stack);
        ForgeHooks.setCraftingPlayer(player);
        automatEntity.consumeIngredients(player);
        automatEntity.updateOutput();
        ForgeHooks.setCraftingPlayer(null);
    }
}
