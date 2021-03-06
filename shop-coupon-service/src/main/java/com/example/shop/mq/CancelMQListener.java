package com.example.shop.mq;

import com.alibaba.fastjson.JSON;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.entity.MQEntity;
import com.example.shop.mapper.TradeCouponMapper;
import com.example.shop.pojo.TradeCoupon;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",
        consumerGroup = "${mq.order.consumer.group.name}",
        messageModel = MessageModel.BROADCASTING) //广播模式：所有的消费者都需要消费消息
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeCouponMapper couponMapper;

    @Override
    public void onMessage(MessageExt message) {
        try {
            //1. 解析消息内容
            String body = new String(message.getBody(), "UTF-8");
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            log.info("couponService【优惠券回退】 -- 接收到消息");
            if (mqEntity.getCouponId() != null) {
                //2. 查询优惠券信息
                TradeCoupon coupon = couponMapper.selectByPrimaryKey(mqEntity.getCouponId());
                //3.更改优惠券状态
                coupon.setIsUsed(ShopCode.SHOP_COUPON_UNUSED.getCode());
                coupon.setUsedTime(null);
                coupon.setOrderId(null);
                couponMapper.updateByPrimaryKey(coupon);
            }
            log.info("couponService【优惠券回退】-- 回退优惠券【trade_coupon】成功");
        } catch (UnsupportedEncodingException e) {
            log.info("couponService【优惠券回退】 -- 回退优惠券【trade_coupon】异常：{}", e);
        }
    }

}
