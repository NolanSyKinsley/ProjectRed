package mrtjp.projectred.core;

import mrtjp.projectred.ProjectRedCore;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class RecipeDrawPlate implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return getCraftingResult(inv) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            // TODO FIX THIS
        }
        return null;
    }

    @Override
    public int getRecipeSize() {
        return 3;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(ProjectRedCore.itemDrawPlate, 1);
    }

}
