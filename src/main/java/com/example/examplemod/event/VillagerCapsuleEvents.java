package com.example.examplemod.event;

import com.example.examplemod.Config;
import com.example.examplemod.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 村民胶囊相关的事件处理。
 * - Shift + 右键村民 → 收进胶囊
 * - 右键方块 → 从胶囊中放出村民
 * 支持通过配置文件开关整体启用/禁用。
 */
@Mod.EventBusSubscriber(modid = "abouttrade")
public class VillagerCapsuleEvents {

    // -------------------------------------------------------------------
    // 收集：Shift + 右键村民
    // -------------------------------------------------------------------
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // 配置禁用时忽略
        if (!Config.ENABLE_VILLAGER_CAPSULE.get()) return;

        if (event.getEntity().level().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!event.getEntity().isShiftKeyDown()) return;

        Entity target = event.getTarget();
        if (!(target instanceof Villager villager)) return;

        Player player = event.getEntity();
        if (player.isSpectator()) return;

        // ---- 安全检查 ----

        // 1. 正在和某个玩家交易的村民不能收集
        if (isTradingWith(villager)) {
            player.displayClientMessage(
                    Component.translatable("abouttrade.capsule.fail.trading"), true);
            event.setCanceled(true);
            return;
        }

        // 2. 幼年村民是否允许收集由配置决定
        if (villager.isBaby() && !Config.ALLOW_BABY_VILLAGER.get()) {
            player.displayClientMessage(
                    Component.translatable("abouttrade.capsule.fail.baby"), true);
            event.setCanceled(true);
            return;
        }

        // ---- 收集逻辑 ----

        CompoundTag villagerTag = new CompoundTag();
        villager.saveWithoutId(villagerTag);

        // 移除位置和标识相关字段，防止释放时冲突
        villagerTag.remove("Pos");
        villagerTag.remove("Motion");
        villagerTag.remove("Rotation");
        villagerTag.remove("UUID");
        villagerTag.remove("WorldUUIDMost");
        villagerTag.remove("WorldUUIDLeast");

        ItemStack capsule = new ItemStack(ModItems.VILLAGER_CAPSULE.get());
        CompoundTag itemTag = new CompoundTag();
        itemTag.put("VillagerData", villagerTag);
        capsule.setTag(itemTag);

        villager.discard();

        if (!player.getInventory().add(capsule)) {
            player.drop(capsule, false);
        }

        event.setCanceled(true);
    }

    // -------------------------------------------------------------------
    // 释放：右键方块
    // -------------------------------------------------------------------
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.ENABLE_VILLAGER_CAPSULE.get()) return;

        if (event.getEntity().level().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        if (player.isSpectator()) return;
        if (player.isShiftKeyDown()) return;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.is(ModItems.VILLAGER_CAPSULE.get())) return;

        CompoundTag itemTag = stack.getTag();
        if (itemTag == null || !itemTag.contains("VillagerData")) return;

        CompoundTag villagerTag = itemTag.getCompound("VillagerData");

        Level level = player.level();
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) return;

        villager.load(villagerTag);
        villager.setUUID(UUID.randomUUID());

        BlockPos pos = event.getPos();
        Direction side = event.getFace();
        Vec3 spawnPos = Vec3.atBottomCenterOf(pos.relative(side));
        villager.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0);

        level.addFreshEntity(villager);

        stack.shrink(1);

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
    }

    // -------------------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------------------

    /**
     * 判断村民是否正在与某个玩家交易。
     * 遍历同世界所有玩家的容器菜单，通过反射读取 MerchantMenu 里的 trader 字段，
     * 判断是否有玩家正在与该村民交易。
     */
    private static boolean isTradingWith(Villager villager) {
        for (Player p : villager.level().players()) {
            if (p.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu menu) {
                // 按字段类型查找 trader（Merchant 类型）
                Merchant trader = getTraderFromMenu(menu);
                if (trader == villager) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 通过反射获取 MerchantMenu 中的 trader 字段。
     * 按类型查找，避开 SRG 名称问题。
     */
    private static Merchant getTraderFromMenu(net.minecraft.world.inventory.MerchantMenu menu) {
        for (java.lang.reflect.Field field : net.minecraft.world.inventory.MerchantMenu.class.getDeclaredFields()) {
            if (Merchant.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return (Merchant) field.get(menu);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}