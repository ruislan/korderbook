package com.ruislan.korderbook.kotlin

import com.ruislan.korderbook.Order
import com.ruislan.korderbook.OrderBook
import com.ruislan.korderbook.OrderBookListener
import org.openjdk.jmh.annotations.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

@State(Scope.Benchmark)
open class OrderBookKotlinPerformance {
    private var orderBook: OrderBook? = null
    private var lock: Lock? = null

    @Setup
    fun prepare() {
        orderBook = OrderBookKotlinImpl("simple", object : OrderBookListener() {})
        lock = ReentrantLock()
    }

    @TearDown
    fun teardown() {
        orderBook?.close()
    }

    @Benchmark
    fun placeLimitOrders() {
        lock!!.withLock { orderBook!!.place(nextRandomLimitOrder()) }
    }

    private fun nextRandomLimitOrder(): Order {
        val isBuy = Random.nextBoolean()
        val price = Random.nextLong(1, 100)
        val qty = Random.nextLong(1, 1000)
        return Order(isBuy, price, qty)
    }
}