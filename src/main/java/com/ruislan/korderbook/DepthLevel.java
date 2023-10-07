package com.ruislan.korderbook;

import java.util.Objects;

public class DepthLevel implements Comparable<DepthLevel> {
    private final long price;
    private long orderCount;
    private long totalQty;
    private long lastChangeQty;

    public DepthLevel(long price) {
        this.price = price;
        this.orderCount = 0L;
        this.totalQty = 0L;
        this.lastChangeQty = 0L;
    }

    /**
     * 减少数量，比如部分成交的情况
     */
    protected void decrease(long qty) {
        totalQty -= qty;
    }

    /**
     * 新订单来了
     */
    protected void addOrder(long qty) {
        ++orderCount;
        totalQty += qty;
        lastChangeQty = qty;
    }

    /**
     * 撤单或者填满一个订单的情况下，会减去订单数和其他数量
     */
    protected void closeOrder(long qty) {
        --orderCount;
        totalQty -= qty;
        lastChangeQty = -qty;
    }

    public long getPrice() {
        return price;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public long getTotalQty() {
        return totalQty;
    }

    public long getLastChangeQty() {
        return lastChangeQty;
    }

    public boolean isEmpty() {
        return totalQty == 0L;
    }

    @Override
    public int compareTo(DepthLevel o) {
        return Long.compare(this.price, o.price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof DepthLevel that)) return false;
        return Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price);
    }

}
