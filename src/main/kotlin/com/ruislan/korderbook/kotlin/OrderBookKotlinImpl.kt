package com.ruislan.korderbook.kotlin

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.ruislan.korderbook.Depth
import com.ruislan.korderbook.Order
import com.ruislan.korderbook.OrderBook
import com.ruislan.korderbook.OrderBookListener
import javax.annotation.concurrent.NotThreadSafe
import kotlin.math.min

/**
 * 订货簿
 *
 * 非线程安全
 */
@NotThreadSafe
class OrderBookKotlinImpl(
    private val symbol: String,
    private val listener: OrderBookListener
) : OrderBook {

    /**
     * 订单容器
     */
    private val bids: Multimap<Long, Order> = MultimapBuilder.treeKeys(Comparator<Long> { o1, o2 ->
        if (o1 == 0L && o2 == 0L) return@Comparator 0 // o1, o2都是市价(0)，返回相等（0）
        if (o1 == 0L) return@Comparator -1 // o1是市价(0)，返回o1小于o2（-1），
        if (o2 == 0L) return@Comparator 1 // o2是市价(0)，返回o1大于o2（1）
        compareValues(o2, o1) // o1,o2都不是市价，比较价格
    }).linkedListValues().build()
    private val asks: Multimap<Long, Order> = MultimapBuilder.treeKeys().linkedListValues().build()// 自然排序的话市价（0）总会在最前面

    /**
     * 深度收集器
     */
    private val asksDepth: Depth = Depth(false)
    private val bidsDepth: Depth = Depth(true)

    /**
     * 市场价
     *
     *
     * 最后一次成交价
     */
    private var marketPrice: Long = 0

    override fun open() {
    }

    override fun close() {
        val bidOrders: List<Order> = bids.values().stream().toList()
        val askOrders: List<Order> = asks.values().stream().toList()

        bidOrders.forEach { order: Order -> this.cancel(order) }
        askOrders.forEach { order: Order -> this.cancel(order) }
    }

    /**
     * 取消订单
     *
     * @param order 要取消的订单
     */
    override fun cancel(order: Order) {
        val holds = if (order.isBuy) bids else asks
        val isRemoved = holds.remove(order.price, order)
        if (isRemoved) {
            listener.onCanceled(order)
            // 更新深度
            if (order.isBuy)
                bidsDepth.onOrderCancelled(order.price, order.openQty)
            else
                asksDepth.onOrderCancelled(order.price, order.openQty)
        } else
            listener.onCancelRejected(order, "no order found")
    }

    /**
     * 下单
     *
     *
     * 下单之后会立刻进行匹配,
     * 如果没有完全把订单填满则放入order book，
     * 等待后面的市价或者限价单进行匹配
     *
     * @param order 订单
     */
    override fun place(order: Order) {
        if (order.isFullFilled)
            listener.onRejected(order, "order is full filled")
        else {
            listener.onAccepted(order)
            matchOrder(order)
        }
    }

    /**
     * 匹配
     *
     *
     * 这里不用区分市价单还是限价单，因为市价单已经被放在了两边队列的最前方
     */
    private fun matchOrder(incomingOrder: Order) {
        val oppositeOrders = if (incomingOrder.isBuy) asks else bids
        val orders = if (incomingOrder.isBuy) bids else asks

        // 没有对手方？
        if (oppositeOrders.isEmpty) {
            orders.put(incomingOrder.price, incomingOrder)
            // 更新深度
            if (incomingOrder.isBuy)
                bidsDepth.onOrderPlaced(incomingOrder.price, incomingOrder.openQty)
            else
                asksDepth.onOrderPlaced(incomingOrder.price, incomingOrder.openQty)
        } else {
            val it = oppositeOrders.values().iterator()

            while (!incomingOrder.isFullFilled && it.hasNext()) {
                val oppositeOrder = it.next()
                val canExecute = (incomingOrder.price == oppositeOrder.price || //价格相等肯定可以执行
                        if (incomingOrder.isBuy)
                            !incomingOrder.isLimit || incomingOrder.price >= oppositeOrder.price
                        else
                            !oppositeOrder.isLimit || incomingOrder.price <= oppositeOrder.price)

                if (!canExecute) break

                val crossPrice =
                    when {
                        oppositeOrder.isLimit -> oppositeOrder.price // 如果对手是个限价单，让cross价格等于对手价
                        incomingOrder.isLimit -> incomingOrder.price // 如果对手不是我方是限价单，让价格等于我方价
                        marketPrice > 0 -> marketPrice //如果我方和对方都不是限价单，又有市场价，让价格等于市场价
                        else -> continue
                    } //还没有市场价，这单不能交易了

                val executeQty = min(incomingOrder.openQty, oppositeOrder.openQty)
                incomingOrder.fill(executeQty)
                oppositeOrder.fill(executeQty)

                marketPrice = crossPrice // 设置这次成交价格成为市场价

                listener.onMatched(incomingOrder, oppositeOrder, crossPrice, executeQty)
                listener.onLastPriceChanged(crossPrice)

                if (oppositeOrder.isFullFilled) {
                    it.remove()
                    listener.onFullFilled(oppositeOrder)
                    // 更新深度
                    if (oppositeOrder.isBuy)
                        bidsDepth.onOrderFullFilled(oppositeOrder.price, executeQty)
                    else
                        asksDepth.onOrderFullFilled(oppositeOrder.price, executeQty)
                } else {
                    // 更新深度
                    if (oppositeOrder.isBuy)
                        bidsDepth.onOrderPartialFilled(oppositeOrder.price, executeQty)
                    else
                        asksDepth.onOrderPartialFilled(oppositeOrder.price, executeQty)
                }
            }

            // 所有可能成交的交易都结束了（或者就没有交易），但是进单还没吃满，放入仓库
            if (!incomingOrder.isFullFilled) {
                orders.put(incomingOrder.price, incomingOrder)
                // 更新深度
                if (incomingOrder.isBuy)
                    bidsDepth.onOrderPlaced(incomingOrder.price, incomingOrder.openQty)
                else
                    asksDepth.onOrderPlaced(incomingOrder.price, incomingOrder.openQty)
            }
        }
    }

    /**
     * 价差
     *
     *
     * 是最低卖价和最高卖价之间的差额
     */
    override fun getSpread(): Long {
        val lowestAskPrice = if (asks.isEmpty) 0L else asks.values().first().price
        val highestBidPrice = if (bids.isEmpty) 0L else bids.values().first().price

        return lowestAskPrice - highestBidPrice
    }

    /**
     * 账簿标记
     *
     *
     * 通常是交易的主体双方的代号组成的字符，例如“usd2cny”或者“btc2etc”等
     *
     *
     * 账簿标记是不能更改的，它是识别账簿的唯一标识
     */
    override fun getSymbol(): String = symbol

    /**
     * 当前的市场价
     *
     *
     * 通常是最后一次成交价格
     */
    override fun getMarketPrice(): Long = marketPrice

    /**
     * 买方深度
     */
    override fun getBidsDepth(): Depth = bidsDepth

    /**
     * 卖方深度
     */
    override fun getAsksDepth(): Depth = asksDepth
}