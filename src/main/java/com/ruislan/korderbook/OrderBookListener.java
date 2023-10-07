package com.ruislan.korderbook;

import java.util.EventListener;

public abstract class OrderBookListener implements EventListener {
    public void onCanceled(Order order) {}
    public void  onCancelRejected(Order order, String reason) {}
    public void onLastPriceChanged(long price) {}
    public void onMatched(Order o1, Order o2, long price, long qty) {}
    public void onAccepted(Order order) {}
    public void onFullFilled(Order order) {}
    public void onRejected(Order order, String reason) {}
}
