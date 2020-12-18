package com.example.shop.pojo;

import java.io.Serializable;

public class TradeUserMoneyLogKey implements Serializable {

    private static final long serialVersionUID = -2374795782946685029L;
    private Long userId;
    private Long orderId;
    private Integer moneyLogType;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getMoneyLogType() {
        return moneyLogType;
    }

    public void setMoneyLogType(Integer moneyLogType) {
        this.moneyLogType = moneyLogType;
    }
}