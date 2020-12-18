package com.example.shop.pojo;

import java.io.Serializable;

public class TradeGoodsNumberLogKey implements Serializable {

    private static final long serialVersionUID = 426988493356546076L;
    private Long goodsId;
    private Long orderId;

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}