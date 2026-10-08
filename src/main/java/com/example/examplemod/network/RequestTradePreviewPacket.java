package com.example.examplemod.network;

import com.example.examplemod.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.trading.Merchant;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：请求当前村民所有等级的交易预览。
 */
public class RequestTradePreviewPacket {

    public RequestTradePreviewPacket() {}

    public void toBytes(FriendlyByteBuf buf) {}

    public RequestTradePreviewPacket(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 配置禁用时忽略请求
            if (!Config.ENABLE_PREVIEW.get()) return;

            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 只在村民交易菜单中生效
            if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) return;

            for (Slot slot : merchantMenu.slots) {
                if (slot.container instanceof MerchantContainer merchantContainer) {
                    Merchant merchant = getMerchantFromContainer(merchantContainer);
                    if (!(merchant instanceof Villager villager)) return;

                    VillagerProfession profession = villager.getVillagerData().getProfession();

                    // 从 VillagerTrades.TRADES 获取该职业的所有等级交易
                    var tradesMap = VillagerTrades.TRADES.get(profession);
                    if (tradesMap == null) return;

                    List<List<TradePreviewEntry>> preview = new ArrayList<>();

                    // 遍历 1~5 级
                    for (int level = 1; level <= 5; level++) {
                        List<TradePreviewEntry> levelEntries = new ArrayList<>();
                        VillagerTrades.ItemListing[] listings = tradesMap.get(level);
                        if (listings != null) {
                            for (VillagerTrades.ItemListing listing : listings) {
                                var offer = listing.getOffer(villager, villager.getRandom());
                                if (offer != null) {
                                    levelEntries.add(new TradePreviewEntry(
                                            offer.getCostA().copy(),
                                            offer.getCostB().copy(),
                                            offer.getResult().copy(),
                                            offer.getMaxUses(),
                                            offer.getXp()
                                    ));
                                }
                            }
                        }
                        preview.add(levelEntries);
                    }

                    // 发送预览数据给客户端
                    int villagerLevel = villager.getVillagerData().getLevel();
                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new TradePreviewPacket(villager.getId(), villagerLevel, preview)
                    );
                    break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** 兼容开发环境和生产环境的反射获取 */
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
}