package com.example.shop.mq;

import com.alibaba.fastjson.JSON;
import com.example.api.IUserService;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.entity.MQEntity;
import com.example.shop.pojo.TradeUserMoneyLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;


@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",
        consumerGroup = "${mq.order.consumer.group.name}",
        messageModel = MessageModel.BROADCASTING) //广播模式：所有的消费者都需要消费消息
public class CancelMQListener implements RocketMQListener<MessageExt> {


    @Autowired
    private IUserService userService;

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            //1.解析消息
            String body = new String(messageExt.getBody(), "UTF-8");
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            log.info("userService【用户余额回退】-- 接收到消息");
            //判断订单回退的余额是否合法
            if (mqEntity.getUserMoney() != null && mqEntity.getUserMoney().compareTo(BigDecimal.ZERO) > 0) {
                //2.调用业务层,进行余额修改
                TradeUserMoneyLog userMoneyLog = new TradeUserMoneyLog();
                userMoneyLog.setUseMoney(mqEntity.getUserMoney());
                userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
                userMoneyLog.setUserId(mqEntity.getUserId());
                userMoneyLog.setOrderId(mqEntity.getOrderId());
                userService.updateMoneyPaid(userMoneyLog);
                log.info("userService【用户余额回退】-- 余额回退成功 【trade_user，trade_user_money_log】");
            }
        } catch (UnsupportedEncodingException e) {
            log.info("userService【用户余额回退】-- 余额回退失败 【trade_user，trade_user_money_log】异常：", e);
        }

    }
}
