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
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RefreshTradesPacket {

    public RefreshTradesPacket() {}

    public void toBytes(FriendlyByteBuf buf) {}

    public RefreshTradesPacket(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 配置禁用时忽略请求
            if (!Config.ENABLE_REFRESH.get()) return;

            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 只在村民交易菜单中生效
            if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) return;

            // 遍历菜单中的槽位，找到交易容器
            for (Slot slot : merchantMenu.slots) {
                if (slot.container instanceof MerchantContainer merchantContainer) {

                    Merchant merchant = getMerchantFromContainer(merchantContainer);
                    if (!(merchant instanceof Villager villager)) {
                        player.displayClientMessage(
                                Component.literal("§c未找到村民，无法刷新"), true);
                        return;
                    }

                    // 已交易过的村民锁定，不允许刷新
                    if (villager.getVillagerXp() > 0) {
                        player.displayClientMessage(
                                Component.literal("该村民已与你交易过，交易列表已锁定"),
                                true
                        );
                        return;
                    }

                    try {
                        // 清空旧交易并重新生成
                        MerchantOffers offers = villager.getOffers();
                        offers.clear();
                        updateVillagerTrades(villager);

                        // 同步新交易到客户端
                        player.connection.send(new ClientboundMerchantOffersPacket(
                                merchantMenu.containerId,
                                villager.getOffers(),
                                villager.getVillagerData().getLevel(),
                                villager.getVillagerXp(),
                                true,
                                true
                        ));
                    } catch (Throwable e) {
                        player.displayClientMessage(
                                Component.literal("§c刷新失败，请查看日志"), true);
                        e.printStackTrace();
                    }
                    break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // -------------------------------------------------------------------
    // 反射辅助方法：兼容开发环境（官方名）和生产环境（SRG 名）
    // -------------------------------------------------------------------

    /**
     * 通过字段类型查找 Merchant，完全避开 SRG 名称问题。
     * MerchantContainer 中只有一个类型为 Merchant 的字段。
     */
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

    /**
     * 调用 Villager 的 updateTrades 方法。
     * 依次尝试 SRG 名和开发名，只要有一个成功就返回。
     */
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
}