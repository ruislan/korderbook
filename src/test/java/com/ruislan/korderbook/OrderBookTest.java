package com.ruislan.korderbook;

import com.ruislan.korderbook.java.OrderBookJavaImpl;
import com.ruislan.korderbook.kotlin.OrderBookKotlinImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookTest extends OrderBookListener {
    private OrderBook orderBook;
    private AtomicLong totalQty;
    private final OrderGenerator orderGenerator = new OrderGenerator();

    @Override
    public void onMatched(Order o1, Order o2, long price, long qty) {
        System.out.println("order matched: " + o1 + ", " + o2 + ", price: " + price + ", qty: " + qty);
        totalQty.addAndGet(qty);
    }

    @Override
    public void onAccepted(Order order) {
        System.out.println(Thread.currentThread() + " , on accepted: " + order);
    }

    @Override
    public void onCanceled(Order order) {
        System.out.println("on cancel: " + order + ", " + orderBook.getBidsDepth().getFirstLevel());
    }

    @BeforeEach
    public void setup() {
        this.orderBook = new OrderBookJavaImpl("simple", this);
        this.totalQty = new AtomicLong(0);
    }

    @AfterEach
    public void teardown() {
        orderBook.close();
    }

    @Test
    void testDepth() {
        orderBook.place(orderGenerator.nextOrder(true, 10L, 100L));// 没有对手订单，将上订单薄
        assertEquals(10, orderBook.getBidsDepth().getFirstLevel().getPrice());
        orderBook.place(orderGenerator.nextOrder(true, 10L, 50L)); // 没有对手订单，将上订单薄
        assertEquals(2, orderBook.getBidsDepth().getFirstLevel().getOrderCount(), "应该有两个订单");
        assertEquals(150, orderBook.getBidsDepth().getFirstLevel().getTotalQty(), "需求数量应该是 150");
        orderBook.place(orderGenerator.nextOrder(false, 10L, 50L)); // 卖单来了
        assertEquals(100, orderBook.getBidsDepth().getFirstLevel().getTotalQty(), "成交数量 50，应该还剩下数量 100");
        assertEquals(50, orderBook.getBidsDepth().getFirstLevel().getLastChangeQty(), "最近变化量应该是 50 ");
        orderBook.place(orderGenerator.nextOrder(false, 11L, 50L)); // 这个11元的卖单不应该成交（订单薄上只有 10 元买单）
        assertEquals(100, orderBook.getBidsDepth().getFirstLevel().getTotalQty(), "买单深度的 10 元价格数量应该无变化");
        assertEquals(50, orderBook.getAsksDepth().getFirstLevel().getTotalQty(), "卖单深度的 11 元价格数量应该是 50");
    }

    @Test
    void testMarketOrder() {
        // 没有订单的时候，入 100 数量的市价单买单
        var marketBuyOrder = orderGenerator.nextOrder(true, 0L, 100L);
        orderBook.place(marketBuyOrder);
        assertEquals(1, orderBook.getBidsDepth().getFirstLevel().getOrderCount(), "应该有一个买单");

        // 此时，有 150 数量的限价卖单进入，由于对手方是 100 个市价单，那么应该直接匹配 100 个，剩余 50 个卖价单。
        var sellLimitOrder = orderGenerator.nextOrder(false, 10L, 150L);
        orderBook.place(sellLimitOrder);
        assertEquals(50, sellLimitOrder.getOpenQty(), "卖单应该有 50 的数量");
        assertEquals(1, orderBook.getAsksDepth().getFirstLevel().getOrderCount(), "应该有一个没有成交完成的卖单订单");
        assertEquals(0, orderBook.getBidsDepth().size(), "买单应该成交完成");

        // 此时，入 50 数量的市价买单，刚好满足剩下的卖单的时候
        marketBuyOrder = orderGenerator.nextOrder(true, 0L, 50L);
        orderBook.place(marketBuyOrder);
        assertEquals(0, sellLimitOrder.getOpenQty());
        assertEquals(0, marketBuyOrder.getOpenQty());
        assertEquals(0, orderBook.getAsksDepth().size());
        assertEquals(0, orderBook.getBidsDepth().size());

        // 不能满足订单的时候
        sellLimitOrder = orderGenerator.nextOrder(false, 10L, 100L);
        orderBook.place(sellLimitOrder);
        marketBuyOrder = orderGenerator.nextOrder(true, 0L, 150L);
        orderBook.place(marketBuyOrder);
        assertEquals(0, sellLimitOrder.getOpenQty());
        assertEquals(50, marketBuyOrder.getOpenQty());
        assertEquals(0, orderBook.getAsksDepth().size());
        assertEquals(1, orderBook.getBidsDepth().getFirstLevel().getOrderCount());

        // 两个订单满足的时候，前面还剩下了50的市价买单
        // 注意这里交易顺序是先来先交易，因为是市价单在等待
        sellLimitOrder = orderGenerator.nextOrder(false, 10L, 10L);
        var sellLimitOrder2 = orderGenerator.nextOrder(false, 9L, 90L);
        orderBook.place(sellLimitOrder);
        orderBook.place(sellLimitOrder2);
        assertEquals(0, sellLimitOrder.getOpenQty());
        assertEquals(50, sellLimitOrder2.getOpenQty());
        assertEquals(0, marketBuyOrder.getOpenQty());
        assertEquals(1, orderBook.getAsksDepth().getFirstLevel().getOrderCount()); // 还有没消耗完的卖单
        assertEquals(0, orderBook.getBidsDepth().size());

        // 两个订单都不能满足的时候，前面限价卖单还有50
        sellLimitOrder2 = orderGenerator.nextOrder(false, 9L, 100L);
        orderBook.place(sellLimitOrder2);
        marketBuyOrder = orderGenerator.nextOrder(true, 0L, 200L);
        orderBook.place(marketBuyOrder);
        assertEquals(0, sellLimitOrder.getOpenQty());
        assertEquals(0, sellLimitOrder2.getOpenQty());
        assertEquals(50, marketBuyOrder.getOpenQty()); // 还有50没成交
        assertEquals(0, orderBook.getAsksDepth().size());
        assertEquals(1, orderBook.getBidsDepth().getFirstLevel().getOrderCount());

        // 现在买方有一个市价买单在等待，而卖方都没有
        // 我们下一个卖方的市价单，应该是按照上一次成交的价格来成交
        final var lastMarketPrice = orderBook.getMarketPrice();
        final var marketSellOrder = orderGenerator.nextOrder(false, 0L, 100L);
        orderBook.place(marketSellOrder);
        assertEquals(lastMarketPrice, orderBook.getMarketPrice());
        assertEquals(0, marketBuyOrder.getOpenQty());
        assertEquals(50, marketSellOrder.getOpenQty()); // 还有50没成交
        assertEquals(1, orderBook.getAsksDepth().getFirstLevel().getOrderCount());
        assertEquals(0, orderBook.getBidsDepth().size());

        // 现在卖方有一个市价单还有50没成交完成
        // 我们现在再下一个卖方的市价单
        // 再下一个买方的市价单，看一下顺序
        final var marketSellOrder2 = orderGenerator.nextOrder(false, 0L, 100L);
        orderBook.place(marketSellOrder2);
        marketBuyOrder = orderGenerator.nextOrder(true, 0L, 50L); //数量和之前的刚刚好
        orderBook.place(marketBuyOrder);
        assertEquals(lastMarketPrice, orderBook.getMarketPrice());
        assertEquals(0, marketBuyOrder.getOpenQty());
        assertEquals(0, marketSellOrder.getOpenQty());
        assertEquals(100, marketSellOrder2.getOpenQty());
        assertEquals(1, orderBook.getAsksDepth().getFirstLevel().getOrderCount());
        assertEquals(0, orderBook.getBidsDepth().size());
    }

    @Test
    void testPlaceLimitOrder() {
        // 没有订单的时候
        var limitBuyOrder = orderGenerator.nextOrder(true, 10L, 100L);
        orderBook.place(limitBuyOrder);
        assertEquals(1, orderBook.getBidsDepth().getFirstLevel().getOrderCount());

        // 单个订单容量足够的时候订单的时候
        var limitSellOrder = orderGenerator.nextOrder(false, 10L, 50L);
        orderBook.place(limitSellOrder);
        assertEquals(50, limitBuyOrder.getOpenQty());
        assertEquals(0, limitSellOrder.getOpenQty());
        assertEquals(0, orderBook.getAsksDepth().size());
        assertEquals(1, orderBook.getBidsDepth().getFirstLevel().getOrderCount());

        // 价格不合适的时候
        limitSellOrder = orderGenerator.nextOrder(false, 11L, 50L);
        orderBook.place(limitSellOrder);
        assertEquals(50, limitBuyOrder.getOpenQty());
        assertEquals(50, limitSellOrder.getOpenQty());
        assertEquals(1, orderBook.getAsksDepth().getFirstLevel().getOrderCount());
        assertEquals(1, orderBook.getBidsDepth().getFirstLevel().getOrderCount());

        // 取消订单的时候
        orderBook.cancel(limitSellOrder);
        assertEquals(50, limitBuyOrder.getOpenQty());
        assertEquals(50, limitSellOrder.getOpenQty());
        assertEquals(0, orderBook.getAsksDepth().size());
        assertEquals(1, orderBook.getBidsDepth().getFirstLevel().getOrderCount());

        // 刚好满足订单的时候
        limitSellOrder = orderGenerator.nextOrder(false, 10L, 50L);
        orderBook.place(limitSellOrder);
        assertEquals(0, limitSellOrder.getOpenQty());
        assertEquals(0, limitBuyOrder.getOpenQty());
        assertEquals(0, orderBook.getAsksDepth().size());
        assertEquals(0, orderBook.getBidsDepth().size());

        // 两个订单满足的时候
        limitSellOrder = orderGenerator.nextOrder(false, 10L, 100L);
        var limitSellOrder2 = orderGenerator.nextOrder(false, 9L, 100L);
        orderBook.place(limitSellOrder);
        orderBook.place(limitSellOrder2);
        limitBuyOrder = orderGenerator.nextOrder(true, 10L, 150L);
        orderBook.place(limitBuyOrder);
        assertEquals(50, limitSellOrder.getOpenQty()); // 这里先应该满足价格低的，然后是价格高的卖单
        assertEquals(0, limitSellOrder2.getOpenQty());
        assertEquals(0, limitBuyOrder.getOpenQty());
        assertEquals(1, orderBook.getAsksDepth().getFirstLevel().getOrderCount()); // 还有没消耗完的卖单
        assertEquals(0, orderBook.getBidsDepth().size());

        // 两个订单都不能满足的时候
        // 注意：前面limitSellOrder还留着50呢！
        limitSellOrder2 = orderGenerator.nextOrder(false, 9L, 100L);
        orderBook.place(limitSellOrder2);
        limitBuyOrder = orderGenerator.nextOrder(true, 10L, 200L);
        orderBook.place(limitBuyOrder);
        assertEquals(50, limitBuyOrder.getOpenQty());// 还有50没成交
        assertEquals(0, limitSellOrder.getOpenQty());
        assertEquals(0, limitSellOrder2.getOpenQty());
        assertEquals(0, orderBook.getAsksDepth().size());
        assertEquals(1, orderBook.getBidsDepth().getFirstLevel().getOrderCount());
    }

    @Test
    void testCancelLimitOrder() {
        final var order = orderGenerator.nextRandomBuyLimitOrder();
        orderBook.place(order);
        orderBook.cancel(order);
        assertTrue(orderBook.getBidsDepth().isEmpty());
    }
}
