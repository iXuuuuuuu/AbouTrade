package com.example.examplemod;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 按钮语言：en（英文）或 zh（中文）
    public static final ForgeConfigSpec.ConfigValue<String> LANGUAGE;
    // 是否启用刷新按钮
    public static final ForgeConfigSpec.BooleanValue ENABLE_REFRESH;
    // 是否启用交易按钮
    public static final ForgeConfigSpec.BooleanValue ENABLE_TRADE;

    static {
        BUILDER.push("general");

        LANGUAGE = BUILDER
                .comment("按钮显示语言，可选 en 或 zh//Language Options en = english zh=chinese")
                .defineInList("language", "zh", List.of("en", "zh"));

        ENABLE_REFRESH = BUILDER
                .comment("是否启用刷新按钮（对未交易过的村民换一批交易）//Enable Disable the refresh button")
                .define("enable_refresh", true);

        ENABLE_TRADE = BUILDER
                .comment("是否启用交易按钮（一次交易到材料或次数上限）//Enable Disable the trade button(A single transaction reaches the limit)")
                .define("enable_trade", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** 根据配置返回按钮文字 */
    public static String text(String zh, String en) {
        return "en".equalsIgnoreCase(LANGUAGE.get()) ? en : zh;
    }
}