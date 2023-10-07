package com.ruislan.korderbook;

import com.google.common.collect.Ordering;

import java.util.TreeMap;

public class Depth {
    public static final int DEFAULT_MAX_LEVEL = 100;
    public static final int TOP_LEVEL = 1;

    private final boolean isBuy;
    private final int maxLevel;
    private final TreeMap<Long, DepthLevel> levels;

    public Depth(boolean isBuy) {
        this(isBuy, DEFAULT_MAX_LEVEL);
    }

    public Depth(boolean isBuy, int maxLevel) {
        this.isBuy = isBuy;
        this.maxLevel = maxLevel > 0 ? maxLevel : DEFAULT_MAX_LEVEL;
        this.levels = new TreeMap<>(this.isBuy ? Ordering.natural().reverse() : Ordering.natural());
    }

    public void onOrderPlaced(long price, long qty) {
        this.levels.computeIfAbsent(price, DepthLevel::new).addOrder(qty);
    }

    public void onOrderCancelled(long price, long qty) {
        internalCloseOrder(price, qty);
    }

    public void onOrderFullFilled(long price, long qty) {
        internalCloseOrder(price, qty);
    }

    private void internalCloseOrder(long price, long qty) {
        final DepthLevel depthLevel = this.levels.get(price);
        if (depthLevel == null) return;
        depthLevel.closeOrder(qty);
        if (depthLevel.isEmpty()) this.levels.remove(price);
    }

    public void onOrderPartialFilled(Long price, Long qty) {
        final DepthLevel depthLevel = this.levels.get(price);
        if (depthLevel == null) return;
        depthLevel.decrease(qty);
    }

    public Boolean isEmpty() {
        return this.levels.isEmpty();
    }

    public int size() {
        return this.levels.size();
    }

    public boolean isBuy() {
        return isBuy;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public DepthLevel getFirstLevel() {
        return this.getDepthLevel(TOP_LEVEL);
    }

    /**
     * 获取某一层深度
     */
    public DepthLevel getDepthLevel(Integer level) {
        int skip;
        if (level < TOP_LEVEL) skip = TOP_LEVEL;
        else if (level > maxLevel) skip = maxLevel;
        else skip = level;
        return this.levels.values().stream().skip((skip - 1)).findFirst().orElse(null);
    }

}
