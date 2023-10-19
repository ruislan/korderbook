package com.ruislan.korderbook.kotlin

import com.ruislan.korderbook.Order
import com.ruislan.korderbook.OrderBook
import com.ruislan.korderbook.OrderBookListener
import org.openjdk.jmh.annotations.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@State(Scope.Benchmark)
open class OrderBookKotlinPubSubPerformance {
    private var consumer: Consumer? = null
    private var queue: BlockingQueue<Order>? = null

    class Consumer(private val queue: BlockingQueue<Order>, private val orderBook: OrderBook) : Runnable {
        private val running: AtomicBoolean = AtomicBoolean(true)

        fun stopRunning() {
            running.set(false)
        }

        override fun run() {
            while (running.get()) {
                val order = queue.take()
                orderBook.place(order)
            }
        }
    }

    @Setup
    fun prepare() {
        queue = LinkedBlockingQueue()
        consumer = Consumer(
            queue!!,
            OrderBookKotlinImpl("simple", object : OrderBookListener() {})
        )
        consumer?.let { Executors.newSingleThreadExecutor().execute(it) }
    }

    @TearDown
    fun teardown() {
        consumer?.stopRunning()
    }

    @Benchmark
    fun placeLimitOrders() {
        queue!!.add(nextRandomLimitOrder())
    }

    private fun nextRandomLimitOrder(): Order {
        val isBuy = Random.nextBoolean()
        val price = Random.nextLong(1, 100)
        val qty = Random.nextLong(1, 1000)
        return Order(isBuy, price, qty)
    }
}