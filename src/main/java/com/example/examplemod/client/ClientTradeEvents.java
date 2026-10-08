package com.example.examplemod.client;

import com.example.examplemod.Config;
import com.example.examplemod.network.NetworkHandler;
import com.example.examplemod.network.RefreshTradesPacket;
import com.example.examplemod.network.RequestTradePreviewPacket;
import com.example.examplemod.network.TradeAllPacket;
import net.minecraft.client.Minecraft;
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

            // 只在启用预览功能时才发送请求，节省性能
            if (Config.ENABLE_PREVIEW.get()) {
                NetworkHandler.INSTANCE.sendToServer(new RequestTradePreviewPacket());
            }

            int guiLeft = merchantScreen.getGuiLeft();
            int guiTop = merchantScreen.getGuiTop();

            int buttonWidth = 28;
            int buttonHeight = 11;
            int gap = 3;

            int buttonX = guiLeft + 104;
            int buttonY = guiTop + 18;

            // 刷新按钮
            if (Config.ENABLE_REFRESH.get()) {
                Button refreshButton = Button.builder(
                        Component.literal(Config.text("刷新", "Refresh")),
                        btn -> NetworkHandler.INSTANCE.sendToServer(new RefreshTradesPacket())
                ).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build();
                event.addListener(refreshButton);
                buttonY += buttonHeight + gap;
            }

            // 交易按钮
            if (Config.ENABLE_TRADE.get()) {
                Button tradeAllButton = Button.builder(
                        Component.literal(Config.text("交易", "Trade")),
                        btn -> NetworkHandler.INSTANCE.sendToServer(new TradeAllPacket())
                ).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build();
                event.addListener(tradeAllButton);
                buttonY += buttonHeight + gap;
            }

            // 预览按钮：只在启用预览功能时才添加
            if (Config.ENABLE_PREVIEW.get()) {
                MerchantScreen finalMerchantScreen = merchantScreen;
                Button previewButton = Button.builder(
                        Component.literal(Config.text("预览", "Preview")),
                        btn -> Minecraft.getInstance().setScreen(
                                new TradePreviewScreen(finalMerchantScreen, TradePreviewCache.getCurrentVillagerLevel())
                        )
                ).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build();
                event.addListener(previewButton);
            }
        }
    }
}