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
    // 是否启用预览按钮
    public static final ForgeConfigSpec.BooleanValue ENABLE_PREVIEW;
    // 是否启用村民胶囊功能
    public static final ForgeConfigSpec.BooleanValue ENABLE_VILLAGER_CAPSULE;
    // 是否允许收集幼年村民
    public static final ForgeConfigSpec.BooleanValue ALLOW_BABY_VILLAGER;

    static {
        BUILDER.push("general");

        LANGUAGE = BUILDER
                .comment("按钮显示语言。可选值：zh（中文）、en（英文）")
                .defineInList("language", "zh", List.of("en", "zh"));

        ENABLE_REFRESH = BUILDER
                .comment("是否启用刷新按钮。设为 false 可隐藏该按钮")
                .define("enable_refresh", true);

        ENABLE_TRADE = BUILDER
                .comment("是否启用交易按钮。设为 false 可隐藏该按钮")
                .define("enable_trade", true);

        ENABLE_PREVIEW = BUILDER
                .comment("是否启用交易预览按钮。设为 false 可隐藏该按钮")
                .define("enable_preview", true);

        ENABLE_VILLAGER_CAPSULE = BUILDER
                .comment("是否启用村民胶囊功能。设为 false 后，Shift + 右键无法收集村民，胶囊也无法释放")
                .define("enable_villager_capsule", true);

        ALLOW_BABY_VILLAGER = BUILDER
                .comment("是否允许收集幼年村民。设为 false 后，Shift + 右键幼年村民将不会收集")
                .define("allow_baby_villager", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** 根据配置返回按钮文字 */
    public static String text(String zh, String en) {
        return "en".equalsIgnoreCase(LANGUAGE.get()) ? en : zh;
    }
}