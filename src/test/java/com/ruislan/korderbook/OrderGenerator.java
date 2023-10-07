package com.ruislan.korderbook;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class OrderGenerator {
    private final AtomicLong totalQty;
    private final Random random;

    public long getTotalQty() {
        return totalQty.get();
    }

    public OrderGenerator() {
        this.totalQty = new AtomicLong(0L);
        random = new Random();
    }

    public Order nextBuyLimitOrder(Long price) {
        long qty = random.nextLong(1, 1000);
        return new Order(true, price, qty);
    }

    public Order nextSellLimitOrder(Long price) {
        long qty = random.nextLong(1, 1000);
        return new Order(false, price, qty);
    }

    public Order nextRandomBuyLimitOrder() {
        long price = random.nextLong(1, 100);
        long qty = random.nextLong(1, 1000);
        return new Order(true, price, qty);
    }

    public Order nextRandomLimitOrder() {
        boolean isBuy = random.nextBoolean();
        long price = random.nextLong(1, 100);
        long qty = random.nextLong(1, 1000);
        return new Order(isBuy, price, qty);
    }

    public Order nextOrder(boolean isBuy, long price, long qty) {
        return new Order(isBuy, price, qty);
    }

    public Order nextRandomSellLimitOrder() {
        long price = random.nextLong(1, 100);
        long qty = random.nextLong(1, 1000);
        return new Order(false, price, qty);
    }
}
