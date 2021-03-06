package com.example.api;


import com.example.shop.entity.Result;
import com.example.shop.pojo.TradeOrder;

public interface IOrderService {

    /**
     * 下单接口
     */
    public Result confirmOrder(TradeOrder order);

}
