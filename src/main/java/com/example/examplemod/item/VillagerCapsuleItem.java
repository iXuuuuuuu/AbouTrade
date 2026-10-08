package com.example.examplemod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 村民胶囊物品。
 * 悬停时显示内部村民的名字、职业、等级。
 * 所有文字使用翻译键，支持中英文切换。
 */
public class VillagerCapsuleItem extends Item {

    public VillagerCapsuleItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag itemTag = stack.getTag();
        if (itemTag == null || !itemTag.contains("VillagerData", Tag.TAG_COMPOUND)) {
            tooltip.add(Component.translatable("abouttrade.capsule.empty").withStyle(s -> s.withColor(0x808080)));
            return;
        }

        CompoundTag villagerTag = itemTag.getCompound("VillagerData");

        // 名字（如果村民有 CustomName）
        if (villagerTag.contains("CustomName", Tag.TAG_STRING)) {
            String nameJson = villagerTag.getString("CustomName");
            try {
                Component name = Component.Serializer.fromJson(nameJson);
                if (name != null) {
                    tooltip.add(Component.translatable("abouttrade.capsule.name", name)
                            .withStyle(s -> s.withColor(0x808080)));
                }
            } catch (Exception ignored) {
                // 名字解析失败就跳过
            }
        }

        // 职业 + 等级
        if (villagerTag.contains("VillagerData", Tag.TAG_COMPOUND)) {
            CompoundTag dataTag = villagerTag.getCompound("VillagerData");
            String profession = dataTag.getString("profession");
            int villagerLevel = dataTag.getInt("level");

            tooltip.add(Component.translatable("abouttrade.capsule.profession", getProfessionComponent(profession))
                    .withStyle(s -> s.withColor(0x808080)));
            tooltip.add(Component.translatable("abouttrade.capsule.level", getLevelComponent(villagerLevel))
                    .withStyle(s -> s.withColor(0x808080)));
        }
    }

    /** 把 "minecraft:librarian" 转成翻译组件 */
    private static Component getProfessionComponent(String profession) {
        if (profession == null || profession.isEmpty() || profession.equals("minecraft:none")) {
            return Component.translatable("abouttrade.profession.none");
        }
        int colon = profession.indexOf(':');
        String name = colon >= 0 ? profession.substring(colon + 1) : profession;
        if (name.isEmpty()) {
            return Component.literal(profession);
        }
        // 使用原版职业翻译键
        return Component.translatable("entity.minecraft.villager." + name);
    }

    /** 把等级数字转成翻译组件 */
    private static Component getLevelComponent(int level) {
        if (level >= 1 && level <= 5) {
            return Component.translatable("abouttrade.level." + level);
        }
        return Component.translatable("abouttrade.level.unknown");
    }
}