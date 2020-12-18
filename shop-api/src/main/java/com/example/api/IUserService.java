package com.example.api;


import com.example.shop.entity.Result;
import com.example.shop.pojo.TradeUser;
import com.example.shop.pojo.TradeUserMoneyLog;

public interface IUserService {

    //获取用户
    TradeUser findOne(Long userId);

    //更新订单：订单付款减去余额，订单退款加余额
    Result updateMoneyPaid(TradeUserMoneyLog userMoneyLog);
}
