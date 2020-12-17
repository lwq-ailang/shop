package com.example.api;


import com.example.shop.entity.Result;
import com.example.shop.pojo.TradeOrder;

public interface IOrderService {

    /**
     * 下单接口
     * @param order
     * @return
     */
    public Result confirmOrder(TradeOrder order);

}
