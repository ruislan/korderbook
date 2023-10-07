package com.ruislan.korderbook;

import java.time.Instant;

public class Order {
    private final boolean isBuy;
    private final long price;
    private final long originQty;
    private long openQty;
    private final long createdAt;
    private long updatedAt;

    /**
     * @param isBuy  是否是买单
     * @param price 请使用最小单位，整数形式，如8.88->888， 8.888 -> 8888
     * @param qty   请使用最小单位，以整数形式，同上
     */
    public Order(boolean isBuy, long price, long qty) {
        this.isBuy = isBuy;
        this.price = price;
        this.originQty = qty;
        this.openQty = qty;
        createdAt = Instant.now().getEpochSecond();
        updatedAt = Instant.now().getEpochSecond();
    }

    public void fill(long qty) {
        openQty -= qty;
        updatedAt = Instant.now().getEpochSecond();
    }

    /**
     * price 为 0 是市价单, price 大于0 是限价单
     */
    public boolean isLimit() {
        return price > 0;
    }

    public boolean isFullFilled() {
        return openQty == 0L;
    }

    public boolean isBuy() {
        return isBuy;
    }

    public long getPrice() {
        return price;
    }

    public long getOriginQty() {
        return originQty;
    }

    public long getOpenQty() {
        return openQty;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
