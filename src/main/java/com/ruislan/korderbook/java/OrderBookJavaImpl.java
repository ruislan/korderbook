package com.ruislan.korderbook.java;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.ruislan.korderbook.Depth;
import com.ruislan.korderbook.Order;
import com.ruislan.korderbook.OrderBook;
import com.ruislan.korderbook.OrderBookListener;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Comparator;
import java.util.List;

@NotThreadSafe
public final class OrderBookJavaImpl implements OrderBook {
    private final String symbol;
    private final OrderBookListener listener;
    private final Multimap<Long, Order> bids;
    private final Multimap<Long, Order> asks;
    private long marketPrice;
    private final Depth asksDepth;
    private final Depth bidsDepth;

    public OrderBookJavaImpl(String symbol, OrderBookListener listener) {
        this.symbol = symbol;
        this.listener = listener;
        this.marketPrice = 0L;
        this.bids = MultimapBuilder.treeKeys((Comparator<Long>) (o1, o2) -> {
            if (o1 == 0L && o2 == 0L) return 0; // o1, o2都是市价(0)，返回相等（0）
            if (o1 == 0L) return -1; // o1是市价(0)，返回o1小于o2（-1），
            if (o2 == 0L) return 1; // o2是市价(0)，返回o1大于o2（1）
            return Long.compare(o2, o1); // o1,o2都不是市价，比较价格
        }).linkedListValues().build();
        this.asks = MultimapBuilder.treeKeys().linkedListValues().build(); // 自然排序的话市价（0）总会在最前面
        this.bidsDepth = new Depth(true);
        this.asksDepth = new Depth(false);
    }


    public void open() {
        // do nothing
    }


    public void close() {
        List<Order> bidOrders = bids.values().stream().toList();
        List<Order> askOrders = asks.values().stream().toList();

        bidOrders.forEach(this::cancel);
        askOrders.forEach(this::cancel);
    }


    public void place(Order order) {
        if (order.isFullFilled()) {
            listener.onRejected(order, "order is full filled");
        } else {
            listener.onAccepted(order);
            matchOrder(order);
        }
    }

    private void matchOrder(Order incomingOrder) {
        final var oppositeOrders = incomingOrder.isBuy() ? asks : bids;
        final var orders = incomingOrder.isBuy() ? bids : asks;

        if (oppositeOrders.isEmpty()) {         // 没有对手方
            orders.put(incomingOrder.getPrice(), incomingOrder); // 放入订单薄等待
            (incomingOrder.isBuy() ? bidsDepth : asksDepth).onOrderPlaced(incomingOrder.getPrice(), incomingOrder.getOpenQty()); // 更新深度
        } else {
            var it = oppositeOrders.values().iterator();
            // 迭代订单，按照价格进行匹配
            while (!incomingOrder.isFullFilled() && it.hasNext()) {
                final Order oppositeOrder = it.next();
                // 可执行条件判断
                // 1. 价格相等肯定可以执行
                // 2. 进单是买单，进单是市价单（市价单总是可以执行除非没有对手单） 或者 进单的价格>=此单价格
                // 3. 进单是卖单，此单是市价单（市价单总是可以执行除非没有对手单） 或者 进单的价格<=此单价格
                final var canExecute = (incomingOrder.getPrice() == oppositeOrder.getPrice()) ||
                        incomingOrder.isBuy() ?
                        !incomingOrder.isLimit() || incomingOrder.getPrice() >= oppositeOrder.getPrice() :
                        !oppositeOrder.isLimit() || incomingOrder.getPrice() <= oppositeOrder.getPrice();
                if (!canExecute) break;

                long crossPrice;
                if (oppositeOrder.isLimit())
                    crossPrice = oppositeOrder.getPrice(); // 如果对手是个限价单，让cross价格等于对手价
                else if (incomingOrder.isLimit())
                    crossPrice = incomingOrder.getPrice(); // 如果对手不是进单是限价单，让价格等于进单价
                else if (marketPrice > 0) crossPrice = marketPrice; //如果进单和对手都不是限价单，又有市场价，让价格等于市场价
                else continue; //还没有市场价，这单不能交易了

                var executeQty = Math.min(incomingOrder.getOpenQty(), oppositeOrder.getOpenQty());
                incomingOrder.fill(executeQty);
                oppositeOrder.fill(executeQty);

                marketPrice = crossPrice; // 设置这次成交价格成为市场价

                listener.onMatched(incomingOrder, oppositeOrder, crossPrice, executeQty);
                listener.onLastPriceChanged(crossPrice);

                // 检查此单成交后的情况
                if (oppositeOrder.isFullFilled()) {
                    it.remove();
                    listener.onFullFilled(oppositeOrder);
                    (oppositeOrder.isBuy() ? bidsDepth : asksDepth).onOrderFullFilled(oppositeOrder.getPrice(), executeQty); // 更新深度
                } else {
                    (oppositeOrder.isBuy() ? bidsDepth : asksDepth).onOrderPartialFilled(oppositeOrder.getPrice(), executeQty); // 更新深度
                }
            }

            // 所有可能成交的交易都结束了（或者就没有交易），但是进单还没吃满，放入仓库
            if (!incomingOrder.isFullFilled()) {
                orders.put(incomingOrder.getPrice(), incomingOrder);
                // 更新深度
                (incomingOrder.isBuy() ? bidsDepth : asksDepth).onOrderPlaced(incomingOrder.getPrice(), incomingOrder.getOpenQty());
            }
        }
    }


    public void cancel(Order order) {
        final var holds = order.isBuy() ? bids : asks;
        final var isRemoved = holds.remove(order.getPrice(), order);
        if (isRemoved) {
            listener.onCanceled(order);
            if (order.isBuy()) {
                bidsDepth.onOrderCancelled(order.getPrice(), order.getOpenQty());
            } else {
                asksDepth.onOrderCancelled(order.getPrice(), order.getOpenQty());
            }
        } else {
            listener.onCancelRejected(order, "order not found");
        }
    }

    public long getSpread() {
        final var lowestOrder = asks.values().stream().findFirst().orElse(null);
        final var highestOrder = bids.values().stream().findFirst().orElse(null);
        final var lowestAskPrice = lowestOrder == null ? 0L : lowestOrder.getPrice();
        final var highestBidPrice = highestOrder == null ? 0L : highestOrder.getPrice();
        return lowestAskPrice - highestBidPrice;
    }


    public String getSymbol() {
        return symbol;
    }


    public long getMarketPrice() {
        return marketPrice;
    }


    public Depth getBidsDepth() {
        return bidsDepth;
    }


    public Depth getAsksDepth() {
        return asksDepth;
    }

}
