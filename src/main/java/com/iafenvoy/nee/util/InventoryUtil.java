package com.iafenvoy.nee.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class InventoryUtil {
    public static boolean hasAllItems(Inventory target, Inventory items) {
        target = copy(target);
        Map<ItemStack, Integer> itemsMap = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.getStack(i);
            itemsMap.put(stack, itemsMap.getOrDefault(stack, 0) + stack.getCount());
        }
        for (int i = 0; i < target.size(); i++) {
            ItemStack targetStack = target.getStack(i);
            for (Map.Entry<ItemStack, Integer> entry : itemsMap.entrySet()) {
                ItemStack stack = entry.getKey();
                int neededCount = entry.getValue();
                if (ItemStack.canCombine(stack, targetStack) && targetStack.getCount() >= neededCount) {
                    itemsMap.put(stack, 0);
                    break;
                }
            }
        }
        for (int count : itemsMap.values())
            if (count > 0)
                return false;
        return true;
    }

    public static void removeItems(Inventory target, Inventory items) {
        Map<ItemStack, Integer> itemsMap = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.getStack(i);
            itemsMap.put(stack, itemsMap.getOrDefault(stack, 0) + stack.getCount());
        }
        for (int i = 0; i < target.size(); i++) {
            ItemStack targetStack = target.getStack(i);
            for (Map.Entry<ItemStack, Integer> entry : itemsMap.entrySet()) {
                ItemStack stack = entry.getKey();
                int neededCount = entry.getValue();
                if (ItemStack.canCombine(stack, targetStack)) {
                    int count = targetStack.getCount();
                    if (count >= neededCount) {
                        targetStack.setCount(count - neededCount);
                        itemsMap.put(stack, 0);
                        break;
                    } else {
                        itemsMap.put(stack, neededCount - count);
                        targetStack.setCount(0);
                    }
                }
            }
        }

        for (Map.Entry<ItemStack, Integer> entry : itemsMap.entrySet())
            if (entry.getValue() > 0)
                throw new IllegalArgumentException("No enough items for removal");
    }

    public static boolean canFitItems(Inventory inventory, Inventory in, Inventory out) {
        inventory = copy(inventory);
        removeItems(out, inventory);
        return canFitAfterRemoval(inventory, in);
    }

    private static boolean canFitAfterRemoval(Inventory inventory, Inventory in) {
        for (int i = 0; i < in.size(); i++) {
            ItemStack inStack = in.getStack(i);
            if (getAvailableSpaceForItem(inventory, inStack) < inStack.getCount())
                return false;
        }
        return true;
    }

    private static int getAvailableSpaceForItem(Inventory inventory, ItemStack stack) {
        int availableSpace = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack inventoryStack = inventory.getStack(i);
            if (inventoryStack == null || ItemStack.canCombine(inventoryStack, stack))
                availableSpace += stack.getMaxCount() - (inventoryStack != null ? inventoryStack.getCount() : 0);
        }
        return availableSpace;
    }

    public static boolean insertItems(Inventory inventory, Inventory insert) {
        for (int i = 0; i < insert.size(); i++) {
            ItemStack insertStack = insert.getStack(i);
            if (insertStack != null) {
                ItemStack copy = insertStack.copy();
                if (!tryAddItemToInventory(inventory, copy))
                    return false;
            }
        }
        return true;
    }

    private static boolean tryAddItemToInventory(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) return true;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack inventoryStack = inventory.getStack(i);
            if (inventoryStack == null || ItemStack.canCombine(inventoryStack, stack)) {
                if (stack.getMaxCount() - (inventoryStack != null ? inventoryStack.getCount() : 0) > 0) {
                    int countToAdd = Math.min(stack.getCount(), stack.getMaxCount() - (inventoryStack != null ? inventoryStack.getCount() : 0));
                    if (inventoryStack == null) inventory.setStack(i, stack.copy());
                    else inventoryStack.setCount(inventoryStack.getCount() + countToAdd);
                    stack.setCount(stack.getCount() - countToAdd);
                    if (stack.getCount() == 0)
                        return true;
                }
            }
        }
        return false;
    }

    public static Inventory copy(Inventory another) {
        Inventory inventory = new SimpleInventory(another.size());
        for (int i = 0; i < another.size(); i++)
            inventory.setStack(i, another.getStack(i).copy());
        return inventory;
    }
}