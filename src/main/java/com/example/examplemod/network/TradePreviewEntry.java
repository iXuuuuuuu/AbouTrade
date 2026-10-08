package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * 用于网络传输的交易预览条目。
 * 只包含渲染预览所需的最基本数据，避免直接序列化复杂的 MerchantOffer。
 */
public class TradePreviewEntry {

    private final ItemStack costA;
    private final ItemStack costB;
    private final ItemStack result;
    private final int maxUses;
    private final int villagerXp;

    public TradePreviewEntry(ItemStack costA, ItemStack costB, ItemStack result, int maxUses, int villagerXp) {
        this.costA = costA;
        this.costB = costB;
        this.result = result;
        this.maxUses = maxUses;
        this.villagerXp = villagerXp;
    }

    // ---- 序列化：写入网络流 ----
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItem(costA);
        buf.writeItem(costB);
        buf.writeItem(result);
        buf.writeInt(maxUses);
        buf.writeInt(villagerXp);
    }

    // ---- 反序列化：从网络流读出 ----
    public static TradePreviewEntry fromBytes(FriendlyByteBuf buf) {
        ItemStack costA = buf.readItem();
        ItemStack costB = buf.readItem();
        ItemStack result = buf.readItem();
        int maxUses = buf.readInt();
        int villagerXp = buf.readInt();
        return new TradePreviewEntry(costA, costB, result, maxUses, villagerXp);
    }

    // ---- Getter ----
    public ItemStack getCostA()  { return costA; }
    public ItemStack getCostB()  { return costB; }
    public ItemStack getResult() { return result; }
    public int getMaxUses()      { return maxUses; }
    public int getVillagerXp()   { return villagerXp; }
}