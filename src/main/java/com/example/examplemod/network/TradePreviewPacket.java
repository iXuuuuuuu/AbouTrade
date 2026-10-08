package com.example.examplemod.network;

import com.example.examplemod.client.TradePreviewCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：发送村民所有等级的交易预览数据。
 */
public class TradePreviewPacket {

    private final int villagerId;
    private final int villagerLevel;
    private final List<List<TradePreviewEntry>> tradesByLevel;

    public TradePreviewPacket() {
        this(-1, 1, new ArrayList<>());
    }

    public TradePreviewPacket(int villagerId, int villagerLevel, List<List<TradePreviewEntry>> tradesByLevel) {
        this.villagerId = villagerId;
        this.villagerLevel = villagerLevel;
        this.tradesByLevel = tradesByLevel;
    }

    // ---- 序列化 ----
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(villagerId);
        buf.writeInt(villagerLevel);
        buf.writeInt(tradesByLevel.size());
        for (List<TradePreviewEntry> levelTrades : tradesByLevel) {
            buf.writeInt(levelTrades.size());
            for (TradePreviewEntry entry : levelTrades) {
                entry.toBytes(buf);
            }
        }
    }

    // ---- 反序列化 ----
    public TradePreviewPacket(FriendlyByteBuf buf) {
        this.villagerId = buf.readInt();
        this.villagerLevel = buf.readInt();
        int levelCount = buf.readInt();
        this.tradesByLevel = new ArrayList<>();
        for (int i = 0; i < levelCount; i++) {
            int tradeCount = buf.readInt();
            List<TradePreviewEntry> levelTrades = new ArrayList<>();
            for (int j = 0; j < tradeCount; j++) {
                levelTrades.add(TradePreviewEntry.fromBytes(buf));
            }
            this.tradesByLevel.add(levelTrades);
        }
    }

    // ---- 处理逻辑：收到后存入客户端缓存 ----
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 【调试日志】收到预览数据时打印
            //System.out.println("[AboutTrade-DEBUG] 客户端：收到预览数据，等级数=" + tradesByLevel.size()
                   // + "，村民等级=" + villagerLevel);

            // 只在客户端执行，避免服务端加载客户端类
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    TradePreviewCache.update(villagerId, villagerLevel, tradesByLevel));
        });
        ctx.get().setPacketHandled(true);
    }

    public int getVillagerLevel() {
        return villagerLevel;
    }
}