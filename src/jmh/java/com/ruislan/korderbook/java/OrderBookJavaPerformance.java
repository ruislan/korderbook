package com.ruislan.korderbook.java;

import com.ruislan.korderbook.Order;
import com.ruislan.korderbook.OrderBook;
import com.ruislan.korderbook.OrderBookListener;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@State(Scope.Benchmark)
public class OrderBookJavaPerformance {
    private OrderBook orderBook;
    private Lock lock;
    private Random random;

    @Setup
    public void prepare() {
        orderBook = new OrderBookJavaImpl("simple", new OrderBookListener() {
        });
        lock = new ReentrantLock();
        random = new Random();
    }

    @TearDown
    public void teardown() {
        orderBook.close();
    }

//    @Benchmark
    public void placeLimitOrders() {
        lock.lock();
        try {
            orderBook.place(nextRandomLimitOrder());
        } finally {
            lock.unlock();
        }
    }

    private Order nextRandomLimitOrder() {
        boolean isBuy = random.nextBoolean();
        long price = random.nextLong(1, 100);
        long qty = random.nextLong(1, 1000);
        return new Order(isBuy, price, qty);
    }
}
