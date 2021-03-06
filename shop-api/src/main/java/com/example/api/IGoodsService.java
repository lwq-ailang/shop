package com.example.api;


import com.example.shop.entity.Result;
import com.example.shop.pojo.TradeGoods;
import com.example.shop.pojo.TradeGoodsNumberLog;

public interface IGoodsService {

    /**
     * 根据ID查询商品对象
     * @param goodsId
     */
    TradeGoods findOne(Long goodsId);

    /**
     * 扣减库存
     * @param goodsNumberLog
     */
    Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog);
}
