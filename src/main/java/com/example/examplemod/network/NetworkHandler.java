package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    // 网络协议版本号
    private static final String PROTOCOL_VERSION = "1";

    // 网络通道：命名空间用模组 ID，路径用 "main"
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExampleMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        // 包1：刷新交易
        INSTANCE.registerMessage(
                id++,
                RefreshTradesPacket.class,
                RefreshTradesPacket::toBytes,
                RefreshTradesPacket::new,
                RefreshTradesPacket::handle
        );

        // 包2：全部交易
        INSTANCE.registerMessage(
                id++,
                TradeAllPacket.class,
                TradeAllPacket::toBytes,
                TradeAllPacket::new,
                TradeAllPacket::handle
        );

        // 包3：交易预览（服务端 -> 客户端）
        INSTANCE.registerMessage(
                id++,
                TradePreviewPacket.class,
                TradePreviewPacket::toBytes,
                TradePreviewPacket::new,
                TradePreviewPacket::handle
        );

        // 包4：请求交易预览（客户端 -> 服务端）
        INSTANCE.registerMessage(
                id++,
                RequestTradePreviewPacket.class,
                RequestTradePreviewPacket::toBytes,
                RequestTradePreviewPacket::new,
                RequestTradePreviewPacket::handle
        );
    }
}