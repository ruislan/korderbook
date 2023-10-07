package com.ruislan.korderbook;

public interface OrderBook {
    void open();

    void close();

    String getSymbol(); //账簿标记,它是识别账簿的唯一标识

    void place(Order order); // 下单,下单之后会立刻进行匹配,如果没有完全把订单填满则放入order book，等待后面的市价或者限价单进行匹配

    void cancel(Order order); //取消订单

    long getSpread(); // 最低卖价减去最高买价

    long getMarketPrice(); //市场价，通常是最后一次成交价格

    Depth getBidsDepth(); //买方深度

    Depth getAsksDepth(); //卖方深度
}
