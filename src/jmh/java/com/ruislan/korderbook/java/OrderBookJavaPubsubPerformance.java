package com.ruislan.korderbook.java;

import com.ruislan.korderbook.Order;
import com.ruislan.korderbook.OrderBook;
import com.ruislan.korderbook.OrderBookListener;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@State(Scope.Benchmark)
public class OrderBookJavaPubsubPerformance {
    private BlockingQueue<Order> queue;
    private Random random;
    private Consumer consumer;

    static class Consumer implements Runnable {
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final OrderBook orderBook;
        private final BlockingQueue<Order> queue;

        public Consumer(BlockingQueue<Order> queue, OrderBook orderBook) {
            this.queue = queue;
            this.orderBook = orderBook;
        }

        public void stopRunning() {
            running.set(false);
            orderBook.close();
        }

        @Override
        public void run() {
            while (running.get()) {
                try {
                    Order order = queue.take();
                    orderBook.place(order);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Setup
    public void prepare() {
        queue = new LinkedBlockingQueue<>();
        random = new Random();
        consumer = new Consumer(queue, new OrderBookJavaImpl("simple", new OrderBookListener() {
        }));
        Executors.newSingleThreadExecutor().execute(consumer);
    }

    @TearDown
    public void teardown() {
        consumer.stopRunning();
    }

    @Benchmark
    public void placeLimitOrders() {
        queue.add(nextRandomLimitOrder()); // pub orders
    }

    private Order nextRandomLimitOrder() {
        boolean isBuy = random.nextBoolean();
        long price = random.nextLong(1, 100);
        long qty = random.nextLong(1, 1000);
        return new Order(isBuy, price, qty);
    }
}
