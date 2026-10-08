package com.example.examplemod;

import com.example.examplemod.item.VillagerCapsuleItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组所有物品的注册中心。
 * DeferredRegister 会在合适的时机自动注册，避免加载顺序问题。
 */
public class ModItems {

    // 创建物品注册器，命名空间用模组 ID
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MOD_ID);

    // 村民胶囊
    public static final RegistryObject<Item> VILLAGER_CAPSULE =
            ITEMS.register("villager_capsule", VillagerCapsuleItem::new);
}