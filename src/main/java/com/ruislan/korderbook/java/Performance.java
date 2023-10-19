package com.ruislan.korderbook.java;

import com.ruislan.korderbook.Order;
import com.ruislan.korderbook.OrderBook;
import com.ruislan.korderbook.OrderBookListener;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Performance {
    static Random random = new Random();

    static void testSingleThread() {
        var time = System.currentTimeMillis();
        System.out.println("prepare 20_000_000 orders per VThread ");
        LinkedBlockingQueue<Order> queue = new LinkedBlockingQueue<>();
        for (var i = 0; i < 1000; i++)
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executor.execute(() -> {
                    for (var j = 0; j < 20000; j++) {
                        boolean isBuy = random.nextBoolean();
                        long price = random.nextLong(1, 100);
                        long qty = random.nextLong(1, 1000);
                        queue.add(new Order(isBuy, price, qty));
                    }
                });
            }
        var elapse = System.currentTimeMillis() - time;
        System.out.println("prepare end, use time: " + elapse + " ms");

        time = System.currentTimeMillis();
        OrderBook orderBook = new OrderBookJavaImpl("simple", new OrderBookListener() {
        });
        while (!queue.isEmpty()) {
            try {
                Order order = queue.take();
                orderBook.place(order);
            } catch (InterruptedException ignored) {
            }
        }
        elapse = System.currentTimeMillis() - time;
        System.out.println("process 20_000_000 orders end, use time: " + elapse + " ms");
        System.out.println();
    }

    static void testLock() {
        var lock = new ReentrantLock();
        OrderBook orderBook = new OrderBookJavaImpl("simple", new OrderBookListener() {
        });
        System.out.println("emit 20_000_000 orders in 1000 VThreads ");
        var time = System.currentTimeMillis();
        var num = 1000;
        var countDown = new CountDownLatch(num);
        for (var i = 0; i < num; i++)
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executor.execute(() -> {
                    for (var j = 0; j < 20000; j++) {
                        var isBuy = random.nextBoolean();
                        var price = random.nextLong(1, 100);
                        var qty = random.nextLong(1, 1000);
                        var order = new Order(isBuy, price, qty);
                        try {
                            lock.lock();
                            orderBook.place(order);
                        } finally {
                            lock.unlock();
                        }
                    }
                    countDown.countDown();
                });
            }

        try {
            countDown.await();
        } catch (InterruptedException ignored) {
        }
        var elapse = System.currentTimeMillis() - time;
        System.out.println("process 20_000_000 orders end, use time: " + elapse + " ms");
        System.out.println();
    }

    static void testPubSub() {
        var time = System.currentTimeMillis();
        var queue = new LinkedBlockingQueue<Order>();
        var running = new AtomicBoolean(true);
        var countDown = new CountDownLatch(2);
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                for (var j = 0; j < 20_000_000; j++) {
                    boolean isBuy = random.nextBoolean();
                    long price = random.nextLong(1, 100);
                    long qty = random.nextLong(1, 1000);
                    queue.add(new Order(isBuy, price, qty));
                }
                running.set(false);
                countDown.countDown();
            });
        }
        OrderBook orderBook = new OrderBookJavaImpl("simple", new OrderBookListener() {
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                while (running.get() || !queue.isEmpty()) {
                    try {
                        var order = queue.take();
                        orderBook.place(order);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                countDown.countDown();
            });
        }
        try {
            countDown.await();
        } catch (InterruptedException ignored) {
        }
        var elapse = System.currentTimeMillis() - time;
        System.out.println("process 20_000_000 orders end, use time: " + elapse + " ms");
        System.out.println();
    }

    public static void main(String[] args) {
        testSingleThread();
        testLock();
        testPubSub();
    }
}
