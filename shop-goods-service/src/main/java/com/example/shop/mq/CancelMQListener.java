package com.example.shop.mq;

import com.alibaba.fastjson.JSON;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.entity.MQEntity;
import com.example.shop.mapper.TradeGoodsMapper;
import com.example.shop.mapper.TradeGoodsNumberLogMapper;
import com.example.shop.mapper.TradeMqConsumerLogMapper;
import com.example.shop.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",
        consumerGroup = "${mq.order.consumer.group.name}",
        messageModel = MessageModel.BROADCASTING) //广播模式：所有的消费者都需要消费消息
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Value("${mq.order.consumer.group.name}")
    private String groupName;

    @Autowired
    private TradeGoodsMapper goodsMapper;
    @Autowired
    private TradeMqConsumerLogMapper mqConsumerLogMapper;
    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        String msgId = null;
        String tags = null;
        String keys = null;
        String body = null;
        try {
            //1. 解析消息内容
            msgId = messageExt.getMsgId();
            tags = messageExt.getTags();
            keys = messageExt.getKeys();
            body = new String(messageExt.getBody(), "UTF-8");
            log.info("goodService -- 接受消息成功");

            //2. 查询消息消费记录
            TradeMqConsumerLogKey primaryKey = create(tags, keys);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(primaryKey);
            log.info("查询消息消费记录【trade_mq_consumer_log】");
            if (mqConsumerLog != null) {
                //3. 判断如果消费过...
                //3.1 获得消息处理状态
                Integer status = mqConsumerLog.getConsumerStatus();

                //处理过...返回
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode().intValue() == status.intValue()) {
                    log.info("goodService--消息:" + msgId + ",已经处理过");
                    return;
                }
                //正在处理...返回
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode().intValue() == status.intValue()) {
                    log.info("goodService--消息:" + msgId + ",正在处理");
                    return;
                }

                //处理失败
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode().intValue() == status.intValue()) {
                    //获得消息处理次数
                    Integer times = mqConsumerLog.getConsumerTimes();
                    if (times > 3) {
                        log.info("goodService--消息:" + msgId + ",消息处理超过3次,不能再进行处理了");
                        return;
                    }

                    //设置状态为正在处理
                    mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());

                    //TODO：使用数据库乐观锁（不加锁）更新 -- 指定更改的条件
                    TradeMqConsumerLogExample example = new TradeMqConsumerLogExample();
                    TradeMqConsumerLogExample.Criteria criteria = example.createCriteria();
                    criteria.andMsgTagEqualTo(mqConsumerLog.getMsgTag());
                    criteria.andMsgKeyEqualTo(mqConsumerLog.getMsgKey());
                    criteria.andGroupNameEqualTo(groupName);
                    //次数保持查询出的次数
                    criteria.andConsumerTimesEqualTo(mqConsumerLog.getConsumerTimes());
                    int r = mqConsumerLogMapper.updateByExampleSelective(mqConsumerLog, example);
                    if (r <= 0) {
                        //TODO：未修改成功,其他线程并发修改（乐观锁，没有抢到资源）
                        log.info("goodService -- 并发修改,稍后处理");
                    }
                }

            } else {
                //4. 判断如果没有消费过...状态改为正在处理，并保存消息消费记录
                insertMqConsumerLog(msgId, tags, keys, body, ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode(), 0);
            }
            //5. 回退库存
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            Long goodsId = mqEntity.getGoodsId();
            //获取商品信息
            TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
            //商品数量 + 回退数量
            goods.setGoodsNumber(goods.getGoodsNumber() + mqEntity.getGoodsNum());
            goodsMapper.updateByPrimaryKey(goods);

            //记录库存操作日志
            saveTradeGoodsNumberLog(mqEntity, goodsId);

            //6. 更新消息的处理状态改为成功
            mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
            mqConsumerLog.setConsumerTimestamp(new Date());
            mqConsumerLogMapper.updateByPrimaryKey(mqConsumerLog);
            log.info("goodService -- 回退库存成功");
        } catch (Exception e) {
            log.info("goodService -- 出现异常：{}",e);
            //消息处理失败，查询消息消费记录
            TradeMqConsumerLogKey primaryKey = create(tags, keys);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(primaryKey);
            log.info("异常处理 -- 查询消息消费记录【trade_mq_consumer_log】");
            if (mqConsumerLog == null) {
                //数据库没有记录，则保存一条
                insertMqConsumerLog(msgId, tags, keys, body, ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode(), 1);
            } else {
                //如果存在，则失败次数加1
                mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes() + 1);
                mqConsumerLogMapper.updateByPrimaryKeySelective(mqConsumerLog);
            }
        }
    }

    //创建消息消费记录对象
    private TradeMqConsumerLogKey create(String tags, String keys) {
        TradeMqConsumerLogKey primaryKey = new TradeMqConsumerLogKey();
        primaryKey.setMsgTag(tags);
        primaryKey.setMsgKey(keys);
        primaryKey.setGroupName(groupName);
        return primaryKey;
    }

    //保存消息消费记录
    private void insertMqConsumerLog(String msgId, String tags, String keys, String body, Integer code, int times) {
        TradeMqConsumerLog mqConsumerLog;//数据库未有记录
        mqConsumerLog = new TradeMqConsumerLog();
        mqConsumerLog.setMsgTag(tags);
        mqConsumerLog.setMsgKey(keys);
        mqConsumerLog.setGroupName(groupName);
        mqConsumerLog.setConsumerStatus(code);
        mqConsumerLog.setMsgBody(body);
        mqConsumerLog.setMsgId(msgId);
        mqConsumerLog.setConsumerTimes(times); //当前失败的次数
        mqConsumerLogMapper.insert(mqConsumerLog);
    }

    //保存商品数量记录表
    private void saveTradeGoodsNumberLog(MQEntity mqEntity, Long goodsId) {
        TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
        goodsNumberLog.setOrderId(mqEntity.getOrderId());
        goodsNumberLog.setGoodsId(goodsId);
        goodsNumberLog.setGoodsNumber(mqEntity.getGoodsNum());
        goodsNumberLog.setLogTime(new Date());
        goodsNumberLogMapper.insert(goodsNumberLog);
        log.info("保存商品数量记录表【trade_goods_number_log】");
    }


}
