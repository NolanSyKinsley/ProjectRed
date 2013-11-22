package mrtjp.projectred.core.inventory;

import java.util.LinkedHashMap;
import java.util.Map;

import mrtjp.projectred.core.BasicUtils;
import mrtjp.projectred.core.utils.ItemKey;
import mrtjp.projectred.core.utils.ItemKeyStack;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import codechicken.lib.vec.BlockCoord;

public class InventoryWrapper {

    IInventory inv;
    
    ISidedInventory sidedInv;
    int side;
    
    int startIndex;
    int endIndex;
    
    int[] slots;
    
    boolean hideOnePerSlot = false;
    boolean hideOnePerType = false;
    
    int fuzzyMathPercentage = 0;
    boolean fuzzyMode = false;
    
    public static InventoryWrapper wrapInventory(IInventory inv) {
        // TODO special inventory wrapping
        return new InventoryWrapper(inv);
    }
    
    private InventoryWrapper(IInventory inv) {
        this.inv = inv;
        if (inv instanceof ISidedInventory)
            sidedInv = (ISidedInventory)inv;
        
        startIndex = 0;
        endIndex = inv.getSizeInventory();
    }
    public InventoryWrapper setSide(int side) {
        this.side = side;
        return this;
    }
    public InventoryWrapper setStart(int start) {
        if (start < 0) start = 0;
        this.startIndex = start;
        return this;
    }
    public InventoryWrapper setEnd(int end) {
        if (end > inv.getSizeInventory()) end = inv.getSizeInventory();
        this.endIndex = end;
        return this;
    }
    public InventoryWrapper setSlotsFromSide() {
        if (sidedInv == null)
            return setSlotsAll();
        
        slots = sidedInv.getAccessibleSlotsFromSide(side);
        return this;
    }
    public InventoryWrapper setSlotsFromRange() {
        slots = new int[endIndex-startIndex];
        for (int i = startIndex; i < endIndex; i++)
            slots[i] = i;
        return this;
    }
    public InventoryWrapper setSlotsAll() {
        slots = new int[inv.getSizeInventory()];
        for (int i = 0; i < inv.getSizeInventory(); i++)
            slots[i] = i;
        return this;
    }
    public InventoryWrapper setFuzzy(boolean flag) {
        fuzzyMode = flag;
        return this;
    }
    public InventoryWrapper setFuzzyPercent(int percent) {
        fuzzyMathPercentage = percent;
        return this;
    }
    public InventoryWrapper setHidePerSlot(boolean flag) {
        hideOnePerSlot = flag;
        if (flag)
            hideOnePerType = false;
        return this;
    }
    public InventoryWrapper setHidePerType(boolean flag) {
        hideOnePerType = flag;
        if (flag)
            hideOnePerSlot = false;
        return this;
    }
    
    /** Inventory Manipulation **/
    
    /**
     * Get a count for how many items of this type can be shoved into the inventory.
     * @param item The item to count free space for. Not manipulated in any way.
     * @return The number of those items this inventory can still take.
     */
    public int getRoomAvailableForItem(ItemKey item) {
        int room = 0;
        ItemStack item2 = item.makeStack(0);
        int slotStackLimit = Math.min(inv.getInventoryStackLimit(), item2.getMaxStackSize());
        for (int slot : slots) {
            ItemStack s = inv.getStackInSlot(slot);
            
            if (!canInsertItem(slot, item2))
                continue;
                
            if (s == null)
                room += slotStackLimit;
            else if (areItemsStackable(s, item2))
                room += (slotStackLimit-s.stackSize);
        }
        return room;
    }
    
    /**
     * Counts how many of those items this inventory contains.
     * @param item The item to count. Not manipulated in any way.
     * @return The number of those items this inventory contains.
     */
    public int getItemCount(ItemKey item) {
        ItemStack item2 = item.makeStack(0);
        int count = 0;
        boolean first = true;
        for (int slot : slots) {
            ItemStack inSlot = inv.getStackInSlot(slot);
            if (inSlot == null)
                continue;
            if (areItemsSame(inSlot, item2) || (fuzzyMode && areItemsFuzzySame(inSlot, item2)) ||
                    (fuzzyMathPercentage > 0 && areItemDamagesInFuzzyGroup(inSlot, item2))) {
                int toAdd = inSlot.stackSize-(hideOnePerSlot || (hideOnePerType && first)?1:0);
                first = false;
                count += toAdd;
            }
        }
        return count;
    }
    
    /**
     * Returns if the given item is in the inventory somewhere.
     * Failfast of getItemCount
     * @param item the item. Not manipulated in any way.
     * @return
     */
    public boolean hasItem(ItemKey item) {
        ItemStack item2 = item.makeStack(0);
        boolean first = true;
        for (int slot : slots) {
            ItemStack inSlot = inv.getStackInSlot(slot);
            if (inSlot == null)
                continue;
            if (areItemsSame(inSlot, item2) || (fuzzyMode && areItemsFuzzySame(inSlot, item2)) ||
                    (fuzzyMathPercentage > 0 && areItemDamagesInFuzzyGroup(inSlot, item2))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Inject the ItemStack into the inventory, starting with merging, then to
     * empty slots.
     * @param item The item to try and merge. Not manipulated in any way.
     * @param doAdd whether or not to actually do the merge. Useful for obtaining a 
     * count of how many items COULD be merged.
     * @return The number of items that were merged in. Equal to the item stack
     * size if all were merged.
     */
    public int injectItem(ItemStack item, boolean doAdd) {
        if (!doAdd)
            return Math.min(getRoomAvailableForItem(ItemKey.get(item)), item.stackSize);
        
        int itemsLeft = item.stackSize;
        int slotStackLimit = Math.min(inv.getInventoryStackLimit(), item.getMaxStackSize());
        
        for (int pass = 0; pass < 2; pass++) {
            for (int slot : slots) {                
                if (!canInsertItem(slot, item))
                    continue;
                
                ItemStack inSlot = inv.getStackInSlot(slot);

                if (inSlot != null && areItemsStackable(item, inSlot)) {
                    int fit = (Math.min(slotStackLimit-inSlot.stackSize, itemsLeft));
                    
                    inSlot.stackSize += fit;
                    itemsLeft -= fit;
                    inv.setInventorySlotContents(slot, inSlot);
                    
                } else if (pass == 1 && inSlot == null) {
                    ItemStack toInsert = item.copy();
                    toInsert.stackSize = Math.min(inv.getInventoryStackLimit(), itemsLeft);
                    itemsLeft -= toInsert.stackSize;
                    inv.setInventorySlotContents(slot, toInsert);
                }
                
                if (itemsLeft == 0)
                    return item.stackSize;
            }
        }
        return item.stackSize - itemsLeft;
    }
    
    /**
     * Extract the item a specified number of times.
     * @param item Item to extract from inventory. Not manipulated in any way.
     * @param toExtract Amount to try to extract.
     * @return Amount extracted.
     */
    public int extractItem(ItemKey item, int toExtract) {
        if (toExtract <= 0)
            return 0;
        ItemStack item2 = item.makeStack(0);
        
        int left = toExtract;
        boolean first = true;
        for (int slot : slots) {
            if (!canExtractItem(slot, item2))
                continue;
            
            ItemStack inSlot = inv.getStackInSlot(slot);
            
            if (inSlot != null && (areItemsSame(inSlot, item2) || (fuzzyMode && areItemsFuzzySame(inSlot, item2)) || (fuzzyMathPercentage > 0 && areItemDamagesInFuzzyGroup(inSlot, item2)))) {

                left -= inv.decrStackSize(slot, Math.min(left, inSlot.stackSize - (hideOnePerSlot || (hideOnePerType && first) ? 1 : 0))).stackSize;
                first = false;
            }
            if (left <= 0)
                return toExtract;
        }
        return toExtract - left;
    }

    /**
     * Return an ordered map of all available [ItemStack, Amount] in the inventory.
     * The actual inventory is not manipulated. Keys are ItemStacks with zero stack size.
     * @return
     */
    public Map<ItemKey, Integer> getAllItemStacks() {
        Map<ItemKey, Integer> items = new LinkedHashMap<ItemKey, Integer>();

        for (int slot : slots) {
            ItemStack inSlot = inv.getStackInSlot(slot);
            if (inSlot == null)
                continue;
            ItemKey key = ItemKey.get(inSlot);
            int stackSize = inSlot.stackSize - (hideOnePerSlot?1:0);
            Integer currentSize = items.get(key);
            if (currentSize == null)
                items.put(key, stackSize-(hideOnePerType?1:0));
            else
                items.put(key, currentSize + stackSize);
        }
        return items;
    }
    
    /** Internal Utils**/
    private boolean canInsertItem(int slot, ItemStack item) {
        return sidedInv == null ? inv.isItemValidForSlot(slot, item) : sidedInv.canInsertItem(slot, item, side);
    }
    private boolean canExtractItem(int slot, ItemStack item) {
        return sidedInv == null ? inv.isItemValidForSlot(slot, item) : sidedInv.canExtractItem(slot, item, side);
    }
    private boolean areItemDamagesInFuzzyGroup(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null || stack2 == null) 
            return stack1 == stack2;
        if (!stack1.isItemStackDamageable() || !stack2.isItemStackDamageable())
            return false;
        double percentDamage1 = (double) stack1.getItemDamage()/stack1.getMaxDamage() * 100;
        double percentDamage2 = (double) stack2.getItemDamage()/stack2.getMaxDamage() * 100;
        boolean isUpperGroup1 = percentDamage1 >= fuzzyMathPercentage;
        boolean isUpperGroup2 = percentDamage2 >= fuzzyMathPercentage;
        return isUpperGroup1 == isUpperGroup2;
    }

    
    /** Static Inventory Utils **/
    public static boolean areItemsStackable(ItemStack stack1, ItemStack stack2) {
        return stack1 == null || stack2 == null || areItemsSame(stack1, stack2) && stack1.isStackable() && stack2.isStackable();
    }
    
    public static boolean areItemsSame(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null || stack2 == null) 
            return stack1 == stack2;
        
        return (stack1.itemID == stack2.itemID && 
                stack2.getItemDamage() == stack1.getItemDamage() && 
                ItemStack.areItemStackTagsEqual(stack2, stack1));
    }
    
    public static boolean areItemsFuzzySame(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null || stack2 == null) 
            return stack1 == stack2;
        
        return (stack1.itemID == stack2.itemID && 
                stack2.getItemDamage() == stack1.getItemDamage());
    }
        
    public static IInventory getInventory(World world, BlockCoord wc) {
        IInventory inv = BasicUtils.getTileEntity(world, wc, IInventory.class);

        if (!(inv instanceof TileEntityChest))
            return inv;
        
        for (int i = 2; i < 6; i++) {
            TileEntityChest chest = BasicUtils.getTileEntity(world, wc.copy().offset(i), TileEntityChest.class);
            if (chest != null)
                return new InventoryLargeChest("Large chest", chest, inv);
        }
        return inv;
    }

}
