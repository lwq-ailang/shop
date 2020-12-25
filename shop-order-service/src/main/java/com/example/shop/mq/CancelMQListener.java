package com.example.shop.mq;

import com.alibaba.fastjson.JSON;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.entity.MQEntity;
import com.example.shop.mapper.TradeOrderMapper;
import com.example.shop.pojo.TradeOrder;
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
        messageModel = MessageModel.BROADCASTING )//广播模式：所有的消费者都需要消费消息
public class CancelMQListener implements RocketMQListener<MessageExt>{

    @Autowired
    private TradeOrderMapper orderMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            //1. 解析消息内容
            String body = new String(messageExt.getBody(),"UTF-8");
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            log.info("orderService【回退订单状态】-- 接受消息成功");
            //2. 查询订单
            TradeOrder order = orderMapper.selectByPrimaryKey(mqEntity.getOrderId());
            //3.更新订单状态为取消
            order.setOrderStatus(ShopCode.SHOP_ORDER_CANCEL.getCode());
            orderMapper.updateByPrimaryKey(order);
            log.info("orderService【回退订单状态】-- 订单状态设置为取消 【trade_order】");
        } catch (UnsupportedEncodingException e) {
            log.info("orderService【回退订单状态】-- 订单取消失败 【trade_order】异常：{}",e);
        }
    }
}
