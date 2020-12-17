package com.example.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.IGoodsService;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.common.exception.CastException;
import com.example.shop.entity.Result;
import com.example.shop.mapper.TradeGoodsMapper;
import com.example.shop.mapper.TradeGoodsNumberLogMapper;
import com.example.shop.pojo.TradeGoods;
import com.example.shop.pojo.TradeGoodsNumberLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
//dubbo生成者
@Service(interfaceClass = IGoodsService.class)
public class GoodsServiceImpl implements IGoodsService {

    @Autowired
    private TradeGoodsMapper goodsMapper;
    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    //根据goodsId查询商品信息
    @Override
    public TradeGoods findOne(Long goodsId) {
        if (goodsId == null) {
            //请求参数有误
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return goodsMapper.selectByPrimaryKey(goodsId);
    }

    //减库存
    @Override
    public Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog) {
        if (goodsNumberLog == null ||
                goodsNumberLog.getGoodsNumber() == null ||
                goodsNumberLog.getOrderId() == null ||
                goodsNumberLog.getGoodsNumber() == null ||
                goodsNumberLog.getGoodsNumber().intValue() <= 0) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsNumberLog.getGoodsId());
        if(goods.getGoodsNumber()<goodsNumberLog.getGoodsNumber()){
            //库存不足
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        //减库存
        goods.setGoodsNumber(goods.getGoodsNumber()-goodsNumberLog.getGoodsNumber());
        goodsMapper.updateByPrimaryKey(goods);


        //记录库存操作日志
        goodsNumberLog.setGoodsNumber(-(goodsNumberLog.getGoodsNumber()));
        goodsNumberLog.setLogTime(new Date());
        goodsNumberLogMapper.insert(goodsNumberLog);

        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }



}
