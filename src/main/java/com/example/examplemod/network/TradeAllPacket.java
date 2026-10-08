package com.example.examplemod.network;

import com.example.examplemod.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TradeAllPacket {

    public TradeAllPacket() {}
    public void toBytes(FriendlyByteBuf buf) {}
    public TradeAllPacket(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!Config.ENABLE_TRADE.get()) return;

            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) return;

            for (Slot slot : merchantMenu.slots) {
                if (slot.container instanceof MerchantContainer container) {
                    MerchantOffer offer = container.getActiveOffer();
                    if (offer == null) {
                        player.displayClientMessage(Component.literal("没有选中任何交易"), true);
                        return;
                    }

                    Merchant merchant = getMerchantFromContainer(container);
                    if (!(merchant instanceof Villager villager)) return;

                    int remainingUses = offer.getMaxUses() - offer.getUses();
                    if (remainingUses <= 0) {
                        player.displayClientMessage(
                                Component.literal("该交易已用完次数，等待村民补货"), true);
                        return;
                    }

                    int maxTrades = calculateMaxTrades(player, container, offer);
                    if (maxTrades <= 0) {
                        player.displayClientMessage(
                                Component.literal("材料不足，无法交易"), true);
                        return;
                    }
                    maxTrades = Math.min(maxTrades, remainingUses);

                    // 循环执行交易，累积经验
                    int totalXpGained = 0;
                    for (int i = 0; i < maxTrades; i++) {
                        executeTrade(player, container, offer);
                        if (offer.shouldRewardExp()) {
                            totalXpGained += 3 + player.getRandom().nextInt(4);  // 3~6 点
                        }
                    }

                    // 结算经验与升级
                    if (totalXpGained > 0) {
                        // ---- 村民经验与升级 ----
                        int newXp = villager.getVillagerXp() + totalXpGained;
                        int oldLevel = villager.getVillagerData().getLevel();
                        int newLevel = getLevelForXp(newXp);

                        if (newLevel > oldLevel) {
                            villager.setVillagerData(villager.getVillagerData().setLevel(newLevel));
                            try {
                                updateVillagerTrades(villager);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                        villager.setVillagerXp(newXp);

                        // ---- 玩家获得经验（仅基础经验，无额外奖励）----
                        player.giveExperiencePoints(totalXpGained);

                        player.displayClientMessage(
                                Component.literal("已交易 " + maxTrades + " 次，获得 " + totalXpGained + " 经验"),
                                true);
                    }

                    // 同步新交易列表到客户端
                    player.connection.send(new ClientboundMerchantOffersPacket(
                            merchantMenu.containerId,
                            villager.getOffers(),
                            villager.getVillagerData().getLevel(),
                            villager.getVillagerXp(),
                            true,
                            true
                    ));

                    break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // -------------------------------------------------------------------
    // 反射辅助方法：兼容开发环境和生产环境
    // -------------------------------------------------------------------

    private static Merchant getMerchantFromContainer(MerchantContainer container) {
        for (java.lang.reflect.Field field : MerchantContainer.class.getDeclaredFields()) {
            if (Merchant.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return (Merchant) field.get(container);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static void updateVillagerTrades(Villager villager) throws Exception {
        String[] candidates = {"m_80642_", "updateTrades"};
        Exception lastException = null;
        for (String name : candidates) {
            try {
                ObfuscationReflectionHelper.findMethod(Villager.class, name).invoke(villager);
                return;
            } catch (Exception e) {
                lastException = e;
            }
        }
        if (lastException != null) throw lastException;
    }

    // -------------------------------------------------------------------
    // 业务逻辑辅助方法
    // -------------------------------------------------------------------

    private int getLevelForXp(int xp) {
        if (xp >= 250) return 5;
        if (xp >= 150) return 4;
        if (xp >= 70) return 3;
        if (xp >= 10) return 2;
        return 1;
    }

    private int calculateMaxTrades(ServerPlayer player, MerchantContainer container, MerchantOffer offer) {
        ItemStack costA = offer.getCostA();
        ItemStack costB = offer.getCostB();

        int trades = countItem(player, container, costA) / costA.getCount();
        if (!costB.isEmpty()) {
            trades = Math.min(trades, countItem(player, container, costB) / costB.getCount());
        }
        return Math.max(trades, 0);
    }

    private int countItem(ServerPlayer player, MerchantContainer container, ItemStack target) {
        int count = 0;
        for (int i = 0; i < 2; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                count += stack.getCount();
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void executeTrade(ServerPlayer player, MerchantContainer container, MerchantOffer offer) {
        removeItem(player, container, offer.getCostA(), offer.getCostA().getCount());
        if (!offer.getCostB().isEmpty()) {
            removeItem(player, container, offer.getCostB(), offer.getCostB().getCount());
        }
        ItemStack result = offer.getResult().copy();
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        offer.increaseUses();
    }

    private void removeItem(ServerPlayer player, MerchantContainer container, ItemStack target, int amount) {
        int remaining = amount;

        for (int i = 0; i < 2 && remaining > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }
}