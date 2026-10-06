package com.example.examplemod.client;

import com.example.examplemod.Config;
import com.example.examplemod.network.NetworkHandler;
import com.example.examplemod.network.RefreshTradesPacket;
import com.example.examplemod.network.TradeAllPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abouttrade", value = Dist.CLIENT)
public class ClientTradeEvents {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof MerchantScreen merchantScreen) {

            int guiLeft = merchantScreen.getGuiLeft();
            int guiTop = merchantScreen.getGuiTop();

            int buttonWidth = 28;
            int buttonHeight = 11;
            int gap = 3;

            int buttonX = guiLeft + 104;
            int buttonY = guiTop + 18;

            // 根据配置决定是否添加刷新按钮
            if (Config.ENABLE_REFRESH.get()) {
                Button refreshButton = Button.builder(
                        Component.literal(Config.text("刷新", "Refresh")),
                        btn -> NetworkHandler.INSTANCE.sendToServer(new RefreshTradesPacket())
                ).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build();
                event.addListener(refreshButton);
                buttonY += buttonHeight + gap;
            }

            // 根据配置决定是否添加交易按钮
            if (Config.ENABLE_TRADE.get()) {
                Button tradeAllButton = Button.builder(
                        Component.literal(Config.text("交易", "Trade")),
                        btn -> NetworkHandler.INSTANCE.sendToServer(new TradeAllPacket())
                ).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build();
                event.addListener(tradeAllButton);
            }
        }
    }
}