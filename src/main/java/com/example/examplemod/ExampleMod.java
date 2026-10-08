package com.example.examplemod;

import com.example.examplemod.network.NetworkHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ExampleMod.MOD_ID)
public class ExampleMod
{
    public static final String MOD_ID = "abouttrade";

    public ExampleMod(FMLJavaModLoadingContext context)
    {
        // 获取模组事件总线
        IEventBus modEventBus = context.getModEventBus();

        // 注册网络包
        NetworkHandler.register();

        // 注册物品
        ModItems.ITEMS.register(modEventBus);

        // 注册配置文件
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}