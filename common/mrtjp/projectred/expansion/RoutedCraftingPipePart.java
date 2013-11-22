package mrtjp.projectred.expansion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mrtjp.projectred.core.BasicGuiUtils;
import mrtjp.projectred.core.inventory.GhostContainer2;
import mrtjp.projectred.core.inventory.GhostContainer2.SlotExtended;
import mrtjp.projectred.core.inventory.InventoryWrapper;
import mrtjp.projectred.core.inventory.SimpleInventory;
import mrtjp.projectred.core.utils.ItemKey;
import mrtjp.projectred.core.utils.ItemKeyStack;
import mrtjp.projectred.core.utils.Pair2;
import mrtjp.projectred.expansion.RequestTreeNode2.CraftingPromise;
import mrtjp.projectred.expansion.RequestTreeNode2.DeliveryPromise;
import mrtjp.projectred.expansion.RequestTreeNode2.ExcessPromise;
import mrtjp.projectred.expansion.RoutedPayload.SendPriority;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import codechicken.core.IGuiPacketSender;
import codechicken.core.ServerUtils;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;

public class RoutedCraftingPipePart extends RoutedPipePart_InvConnect implements IWorldRoutedCrafter {
    
    public SimpleInventory matrix = new SimpleInventory(10, "matrix", 256);
    private DeliveryManager manager = new DeliveryManager();
    public final LinkedList<Pair2<ItemKeyStack, IWorldRoutedRequester>> excess = new LinkedList<Pair2<ItemKeyStack, IWorldRoutedRequester>>();

    public int priority = 0;
    
    private int remainingDelay = operationDelay();
    private int operationDelay() {
        return 40;
    }
    
    protected int itemsToExtract() {
        return 1;
    }
    
    protected int stacksToExtract() {
        return 1;
    }
    
    public void read(MCDataInput packet, int switch_key) {
        if (switch_key == 45)
            priority = packet.readInt();
        else
            super.read(packet, switch_key);
    }
    
    public void priorityUp() {
        int old = priority;
        priority = Math.min(100, priority+1);
        if (old != priority)
            sendPriorityUpdate();
    }
    
    public void priorityDown() {
        int old = priority;
        priority = Math.max(-100, priority-1);
        if (old != priority)
            sendPriorityUpdate();
    }
    
    private void sendPriorityUpdate() {
        if (!world().isRemote)
            tile().getWriteStream(this).writeByte(45).writeInt(priority);
    }
        
    @Override
    public void update() {
        super.update();
        if (--remainingDelay >= 0)
            return;
        remainingDelay = operationDelay();
        
        if (!world().isRemote)
            operationTick();
    }

    private void operationTick() {
        IInventory real = getInventory();
        if (real == null) {
            if (manager.hasOrders())
                manager.sendFailed();
            else 
                excess.clear();
            return;
        }
        int side = getInterfacedSide();
        
        InventoryWrapper inv = InventoryWrapper.wrapInventory(real).setSide(side).setSlotsFromSide();

        List<ItemKeyStack> wanted = getCraftedItems();
        if (wanted == null || wanted.isEmpty())
            return;

        int itemsleft = itemsToExtract();
        int stacksleft = stacksToExtract();
        
        while (itemsleft > 0 && stacksleft > 0 && (manager.hasOrders() || !excess.isEmpty())) {
            Pair2<ItemKeyStack, IWorldRoutedRequester> nextOrder;
            boolean processingOrder=false;
            if (manager.hasOrders()) {
                nextOrder = manager.peek();
                processingOrder = true;
            } else
                nextOrder = excess.getFirst();
            
            ItemKeyStack keyStack = nextOrder.getValue1();
            
            int maxToSend = Math.min(itemsleft, nextOrder.getValue1().stackSize);
            maxToSend = Math.min(nextOrder.getValue1().getStackLimit(), maxToSend);
            int available = inv.extractItem(keyStack.getKey(), maxToSend);
            
            if (available <= 0)
                break;
            
            ItemKey key = keyStack.getKey();
            while (available > 0) {
                int numToSend = Math.min(available, key.getStackLimit());
                numToSend = Math.min(numToSend, nextOrder.getValue1().stackSize);
                if (numToSend == 0)
                    break;
                stacksleft -= 1;
                itemsleft -= numToSend;
                available -= numToSend;
                ItemStack toSend = key.makeStack(numToSend);
                
                if (processingOrder) {
                    queueStackToSend(toSend, side, SendPriority.ACTIVE, nextOrder.getValue2().getRouter().getIPAddress());
                    manager.sendSuccessful(numToSend, false);

                    if (manager.hasOrders())
                        nextOrder = manager.peek();
                    else {
                        processingOrder = false;
                        if (!excess.isEmpty())
                            nextOrder = excess.getFirst();
                    }

                } else {
                    removeExcess(key, numToSend);
                    queueStackToSend(toSend, side, SendPriority.WANDERING, -1);
                }
            }
        }
    }
    
    private void removeExcess(ItemKey item, int amount) {
        Iterator<Pair2<ItemKeyStack, IWorldRoutedRequester>> iter = excess.iterator();
        while (iter.hasNext()) {
            ItemKeyStack stack = iter.next().getValue1();
            if (stack.getKey().equals(item)) {
                if (amount > stack.stackSize) {
                    amount -= stack.stackSize;
                    iter.remove();
                    if (amount <= 0)
                        return;
                } else {
                    stack.stackSize -= amount;
                    break;
                }
            }
        }
    }
    
    @Override
    public boolean activate(EntityPlayer player, MovingObjectPosition hit, ItemStack item) {
        if (super.activate(player, hit, item))
            return true;
        
        openGui(player);
        return true;
    }
    
    public void openGui(EntityPlayer player) {
        if (world().isRemote) return;
        
        ServerUtils.openSMPContainer((EntityPlayerMP) player, createContainer(player), new IGuiPacketSender() {
            @Override
            public void sendPacket(EntityPlayerMP player, int windowId) {
                PacketCustom p = new PacketCustom(ExpansionSPH.channel, NetConstants.gui_CraftingPipe_open);
                p.writeCoord(x(), y(), z());
                p.writeByte(windowId);
                p.sendToPlayer(player);
                p.writeInt(priority);
            }
        });
    }
    
    public Container createContainer(EntityPlayer player) {
        GhostContainer2 ghost = new GhostContainer2(player.inventory);
        int slot = 0;
        for (Pair2<Integer, Integer> p : BasicGuiUtils.createSlotArray(26, 26, 3, 3, 0, 0))
            ghost.addCustomSlot(new SlotExtended(matrix, slot++, p.getValue1(), p.getValue2()).setGhosting(true));
        
        ghost.addCustomSlot(new SlotExtended(matrix, slot++, 117, 63).setGhosting(true));
        
        ghost.addPlayerInventory(8, 118);
        return ghost;
    }

    @Override
    public void save(NBTTagCompound tag) {
        super.save(tag);
        matrix.save(tag);
    }
    
    @Override
    public void load(NBTTagCompound tag) {
        super.load(tag);
        matrix.load(tag);
    }

    
    @Override
    public void requestPromises(RequestTreeNode2 request, int existingPromises) {
        if (excess.isEmpty()) return;
        
        ItemKey requestedItem = request.getRequestedPackage();
        List<ItemKeyStack> providedItem = getCraftedItems();
        for (ItemKeyStack item : providedItem)
            if (item.getKey() == requestedItem)
                return;
        
        if (!providedItem.contains(requestedItem))
            return;
        
        int remaining = 0;
        for (Pair2<ItemKeyStack, IWorldRoutedRequester> extra : excess)
            if (extra.getValue1().getKey() == requestedItem)
                remaining += extra.getValue1().stackSize;
        
        remaining -= existingPromises;
        if (remaining <= 0)
            return;

        ExcessPromise promise = new ExcessPromise();
        promise.thePackage = requestedItem;
        promise.deliveryCount = Math.min(remaining, request.getMissingCount());
        promise.sender = this;
        promise.used = true;
        request.addPromise(promise);
    }

    @Override
    public void deliverPromises(DeliveryPromise promise, IWorldRoutedRequester requester) {
        if (promise instanceof ExcessPromise)
            removeExcess(promise.thePackage, promise.deliveryCount);
        
        manager.addOrder(ItemKeyStack.get(promise.thePackage, promise.deliveryCount), requester);
    }

    @Override
    public void getBroadcastedItems(Map<ItemKey, Integer> map) {
    }

    @Override
    public CraftingPromise requestCraftPromise(ItemKey item) {
        List<ItemKeyStack> stack = getCraftedItems();
        if (stack == null)
            return null;
        
        boolean found = false;
        ItemKeyStack craftingStack = null;
        for (ItemKeyStack craftable : stack) {
            craftingStack = craftable;
            if (craftingStack.getKey().equals(item)) {
                found = true;
                break;
            }
        }
        if (!found)
            return null;

        IWorldRoutedRequester[] requesters = new IWorldRoutedRequester[9];
        for (int i = 0; i < 9; i++)
            requesters[i] = this;
        
        CraftingPromise promise = new CraftingPromise(craftingStack, this, priority);
        for (int i = 0; i < 9; i++) {
            ItemKeyStack keystack = ItemKeyStack.get(matrix.getStackInSlot(i));
            if (keystack == null || keystack.stackSize <= 0)
                continue;
            promise.addIngredient(keystack, requesters[i]);
        }
        
        return promise;
    }

    @Override
    public void registerExcess(DeliveryPromise promise) {
        ItemKeyStack keystack = ItemKeyStack.get(promise.thePackage, promise.deliveryCount);
        excess.add(new Pair2<ItemKeyStack, IWorldRoutedRequester>(keystack, null));
    }

    @Override
    public List<ItemKeyStack> getCraftedItems() {
        List<ItemKeyStack> list = new ArrayList<ItemKeyStack>(1);
        ItemStack stack = matrix.getStackInSlot(9);
        if (stack != null)
            list.add(ItemKeyStack.get(stack));
        return list;
    }

    @Override
    public int getWorkLoad() {
        return manager.getTotalDeliveryCount();
    }
    
    @Override
    public String getType() {
        return "pr_rcrafting";
    }
}
