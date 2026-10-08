package com.example.examplemod.client;

import com.example.examplemod.network.TradePreviewEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端侧的交易预览缓存。
 * 服务端通过 TradePreviewPacket 发送数据过来后，暂存在这里，
 * 供预览界面渲染时读取。
 */
public class TradePreviewCache {

    // 按等级（1~5）存储交易列表。索引 0 = 等级 1，索引 4 = 等级 5
    private static final List<List<TradePreviewEntry>> TRADES_BY_LEVEL = new ArrayList<>();

    // 当前缓存对应的村民实体 ID，用于切换村民时清空缓存
    private static int currentVillagerId = -1;

    // 当前村民的等级（1~5）
    private static int currentVillagerLevel = 1;

    /**
     * 服务端发送预览数据后，客户端调用此方法更新缓存。
     */
    public static void update(int villagerId, int villagerLevel, List<List<TradePreviewEntry>> tradesByLevel) {
        currentVillagerId = villagerId;
        currentVillagerLevel = villagerLevel;
        TRADES_BY_LEVEL.clear();
        TRADES_BY_LEVEL.addAll(tradesByLevel);
    }

    /**
     * 获取指定等级的交易列表（等级从 1 开始）。
     */
    public static List<TradePreviewEntry> getTradesForLevel(int level) {
        if (level < 1 || level > TRADES_BY_LEVEL.size()) {
            return Collections.emptyList();
        }
        return TRADES_BY_LEVEL.get(level - 1);
    }

    public static int getCurrentVillagerId() {
        return currentVillagerId;
    }

    public static int getCurrentVillagerLevel() {
        return currentVillagerLevel;
    }

    public static boolean hasData(int villagerId) {
        return currentVillagerId == villagerId && !TRADES_BY_LEVEL.isEmpty();
    }

    public static void clear() {
        TRADES_BY_LEVEL.clear();
        currentVillagerId = -1;
        currentVillagerLevel = 1;
    }
}