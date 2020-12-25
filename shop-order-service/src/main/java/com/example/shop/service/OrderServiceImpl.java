package com.example.shop.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.example.api.ICouponService;
import com.example.api.IGoodsService;
import com.example.api.IOrderService;
import com.example.api.IUserService;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.common.exception.CastException;
import com.example.shop.common.utils.IDWorker;
import com.example.shop.entity.MQEntity;
import com.example.shop.entity.Result;
import com.example.shop.mapper.TradeOrderMapper;
import com.example.shop.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Component
//dubbo生产者
@Service(interfaceClass = IOrderService.class)
public class OrderServiceImpl implements IOrderService {

    //MQ -- 订单创建失败，需要回滚
    @Value("${mq.order.topic}")
    private String topic;
    @Value("${mq.order.tag.cancel}")
    private String tag;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    //dubbo
    @Reference
    private IGoodsService goodsService;
    @Reference
    private IUserService userService;
    @Reference
    private ICouponService couponService;
    //本地
    @Autowired
    private TradeOrderMapper orderMapper;
    //雪花算法
    @Autowired
    private IDWorker idWorker;

    //下单接口
    @Override
    public Result confirmOrder(TradeOrder order) {
        //1.校验订单
        checkOrder(order);
        //2.生成预订单
        Long orderId = savePreOrder(order);
        try {
            //TODO：保证原子性：3,4,5,6,7保持一致性，一起成功，一起失败
            //3.扣减库存
            reduceGoodsNum(order);
            //4.扣减优惠券
            updateCouponStatus(order);
            //5.使用余额
            reduceMoneyPaid(order);

            //模拟异常抛出
            //CastException.cast(ShopCode.SHOP_FAIL);

            //6.确认订单
            updateOrderStatus(order);
            //7.返回成功状态
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {
            //1.确认订单失败,发送消息
            MQEntity mqEntity = new MQEntity();
            mqEntity.setOrderId(orderId);
            mqEntity.setUserId(order.getUserId());
            mqEntity.setUserMoney(order.getMoneyPaid());
            mqEntity.setGoodsId(order.getGoodsId());
            mqEntity.setGoodsNum(order.getGoodsNumber());
            mqEntity.setCouponId(order.getCouponId());
            //2.返回订单确认失败消息
            try {
                sendCancelOrder(topic,tag,order.getOrderId().toString(), JSON.toJSONString(mqEntity));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }
    }

    /**
     * 1.校验订单
     *    1)校验订单是否存在
     *    2)校验订单中的商品是否存在
     *    3)校验下单用户是否存在
     *    4)校验商品单价是否合法
     *    5)校验订单商品数量是否合法
     */
    private void checkOrder(TradeOrder order) {
        //1.校验订单是否存在
        if (order == null) {
            //订单为空，订单无效
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        //2.校验订单中的商品是否存在：根据商品id获取商品信息
        TradeGoods goods = goodsService.findOne(order.getGoodsId());
        if (goods == null) {
            //商品不存在
            CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);
        }
        //3.校验下单用户是否存在
        TradeUser user = userService.findOne(order.getUserId());
        if (user == null) {
            //用户不存在
            CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
        }
        //4.校验商品单价是否合法
        if (order.getGoodsPrice().compareTo(goods.getGoodsPrice()) != 0) {
            CastException.cast(ShopCode.SHOP_GOODS_PRICE_INVALID);
        }
        //5.校验订单商品数量是否合法
        if (order.getGoodsNumber() >= goods.getGoodsNumber()) {
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        log.info("校验订单通过");
    }

    /**
     * 2.生成预订单
     *    1)设置订单状态为不可见
     *    2)设置订单ID
     *    3)核算订单运费
     *    4)核算订单总金额是否合法
     *    5)判断用户是否使用余额
     *    6)判断用户是否使用优惠券
     *    7)核算订单支付金额：订单总金额-余额-优惠券金额
     *    8)设置下单时间
     *    9)保存订单到数据库
     *    10)返回订单ID
     */
    private Long savePreOrder(TradeOrder order) {
        //1. 设置订单状态为不可见【0, "订单未确认，1, "订单已经确认"，2, "订单已取消"，3, "订单无效"，4, "订单已退货"】
        order.setOrderStatus(ShopCode.SHOP_ORDER_NO_CONFIRM.getCode());
        //2. 设置订单ID - 使用雪花算法，防止分库分表订单id重复
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        //3. 核算订单运费
        BigDecimal shippingFee = calculateShippingFee(order.getOrderAmount());
        if(order.getShippingFee().compareTo(shippingFee)!=0){
            CastException.cast(ShopCode.SHOP_ORDER_SHIPPINGFEE_INVALID);
        }
        //4. 核算订单总金额是否合法
        BigDecimal orderAmount = order.getGoodsPrice().multiply(new BigDecimal(order.getGoodsNumber()));
        orderAmount.add(shippingFee); //订单总价 + 运费价格
        if(order.getOrderAmount().compareTo(orderAmount)!=0){
            CastException.cast(ShopCode.SHOP_ORDERAMOUNT_INVALID);
        }
        //5.判断用户是否使用余额
        BigDecimal moneyPaid = order.getMoneyPaid();
        if(moneyPaid!=null){
            //5.1 订单中余额是否合法
            int r = moneyPaid.compareTo(BigDecimal.ZERO);
            //余额小于0
            if(r==-1){
                CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
            }
            //余额大于0
            if(r==1){
                //获取用户的余额
                TradeUser user = userService.findOne(order.getUserId());
                if(moneyPaid.compareTo(new BigDecimal(user.getUserMoney()))==1){
                    CastException.cast(ShopCode.SHOP_MONEY_PAID_INVALID);
                }
            }
        }else{
            order.setMoneyPaid(BigDecimal.ZERO);
        }
        //6.判断用户是否使用优惠券
        Long couponId = order.getCouponId();
        if(couponId!=null){
            TradeCoupon coupon = couponService.findOne(couponId);
            //6.1 判断优惠券是否存在
            if(coupon==null){
                CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
            }
            //6.2 判断优惠券是否已经被使用
            if(coupon.getIsUsed().intValue()==ShopCode.SHOP_COUPON_ISUSED.getCode().intValue()){
                CastException.cast(ShopCode.SHOP_COUPON_ISUSED);
            }
            order.setCouponPaid(coupon.getCouponPrice());
        }else{
            order.setCouponPaid(BigDecimal.ZERO);
        }
        //7.核算订单支付金额 = 订单总金额-余额-优惠券金额
        BigDecimal payAmount = order.getOrderAmount().subtract(order.getMoneyPaid()).subtract(order.getCouponPaid());
        order.setPayAmount(payAmount);
        //8.设置下单时间
        order.setAddTime(new Date());
        //9.保存订单到数据库
        orderMapper.insert(order);
        //10.返回订单ID
        return orderId;
    }

    /**
     * 2.1.核算运费
     */
    private BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        //超过100 就包邮
        if(orderAmount.compareTo(new BigDecimal(100))==1){
            return BigDecimal.ZERO;
        }else{
            return new BigDecimal(10);
        }

    }

    /**
     * 3.扣减库存 -- 保证原子性
     */
    private void reduceGoodsNum(TradeOrder order) {
        TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
        goodsNumberLog.setOrderId(order.getOrderId());
        goodsNumberLog.setGoodsId(order.getGoodsId());
        goodsNumberLog.setGoodsNumber(order.getGoodsNumber());
        Result result = goodsService.reduceGoodsNum(goodsNumberLog);
        if(result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())){
            CastException.cast(ShopCode.SHOP_REDUCE_GOODS_NUM_FAIL);
        }
        log.info("订单:"+order.getOrderId()+"扣减库存成功");
    }

    /**
     * 4.使用优惠券 -- 保证原子性
     */
    private void updateCouponStatus(TradeOrder order) {
        if(order.getCouponId()!=null){
            //获取优惠券信息
            TradeCoupon coupon = couponService.findOne(order.getCouponId());
            coupon.setOrderId(order.getOrderId());
            coupon.setIsUsed(ShopCode.SHOP_COUPON_ISUSED.getCode());
            coupon.setUsedTime(new Date());
            //更新优惠券状态
            Result result =  couponService.updateCouponStatus(coupon);
            if(result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())){
                CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
            }
            log.info("订单:"+order.getOrderId()+",使用优惠券");
        }
    }

    /**
     * 5.扣减余额 -- 保证原子性
     */
    private void reduceMoneyPaid(TradeOrder order) {
        if(order.getMoneyPaid()!=null && order.getMoneyPaid().compareTo(BigDecimal.ZERO)==1){
            //用户余额日志表
            TradeUserMoneyLog userMoneyLog = new TradeUserMoneyLog();
            userMoneyLog.setOrderId(order.getOrderId());
            userMoneyLog.setUserId(order.getUserId());
            userMoneyLog.setUseMoney(order.getMoneyPaid());
            //订单的状态--非常重要
            userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_PAID.getCode());
            Result result = userService.updateMoneyPaid(userMoneyLog);
            if(result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())){
                CastException.cast(ShopCode.SHOP_USER_MONEY_REDUCE_FAIL);
            }
            log.info("订单:"+order.getOrderId()+",扣减余额成功");
        }
    }

    /**
     * 6.确认订单 -- 保证原子性
     */
    private void updateOrderStatus(TradeOrder order) {
        order.setOrderStatus(ShopCode.SHOP_ORDER_CONFIRM.getCode());
        order.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        order.setConfirmTime(new Date());
        //更新最终订单
        int r = orderMapper.updateByPrimaryKey(order);
        if(r<=0){
            CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
        }
        log.info("订单:"+order.getOrderId()+"确认订单成功");
    }

    /**
     * 往MQ发送订单确认失败消息
     */
    private void sendCancelOrder(String topic, String tag, String keys, String body) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message message = new Message(topic,tag,keys,body.getBytes());
        SendResult send = rocketMQTemplate.getProducer().send(message);
        log.info("第一条mq={}",send);

        //rocketMQTemplate.send(topic + ":" + tag, MessageBuilder.withPayload(body).setHeader(MessageConst.PROPERTY_KEYS,keys).build());
        //log.info("第二条");
    }

}
