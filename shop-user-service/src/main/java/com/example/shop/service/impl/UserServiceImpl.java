package com.example.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.IUserService;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.common.exception.CastException;
import com.example.shop.entity.Result;
import com.example.shop.mapper.TradeUserMapper;
import com.example.shop.mapper.TradeUserMoneyLogMapper;
import com.example.shop.pojo.TradeUser;
import com.example.shop.pojo.TradeUserMoneyLog;
import com.example.shop.pojo.TradeUserMoneyLogExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Component
//dubbo生产者
@Service(interfaceClass = IUserService.class)
public class UserServiceImpl implements IUserService{

    @Autowired
    private TradeUserMapper userMapper;
    @Autowired
    private TradeUserMoneyLogMapper userMoneyLogMapper;

    //查找用户
    @Override
    public TradeUser findOne(Long userId) {
        if(userId==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return userMapper.selectByPrimaryKey(userId);
    }

    @Override
    public Result updateMoneyPaid(TradeUserMoneyLog userMoneyLog) {
        //1.校验参数是否合法
        if(userMoneyLog==null ||
                userMoneyLog.getUserId()==null ||
                userMoneyLog.getOrderId()==null ||
                userMoneyLog.getUseMoney()==null||
                userMoneyLog.getUseMoney().compareTo(BigDecimal.ZERO)<=0){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }

        //2.余额使用日志去查询订单，判断是否已经支付
        TradeUserMoneyLogExample userMoneyLogExample = new TradeUserMoneyLogExample();
        TradeUserMoneyLogExample.Criteria criteria = userMoneyLogExample.createCriteria();
        //构建查询条件
        criteria.andOrderIdEqualTo(userMoneyLog.getOrderId());
        criteria.andUserIdEqualTo(userMoneyLog.getUserId());
        //统计日志是否有该用户的订单记录
        int r = userMoneyLogMapper.countByExample(userMoneyLogExample);
        //获取用户--用来扣减余额和回退余额
        TradeUser tradeUser = userMapper.selectByPrimaryKey(userMoneyLog.getUserId());
        //3.扣减余额...
        if(userMoneyLog.getMoneyLogType().intValue()==ShopCode.SHOP_USER_MONEY_PAID.getCode().intValue()){
            //已经付款
            if(r>0){
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
            }
            //减余额
            tradeUser.setUserMoney(new BigDecimal(tradeUser.getUserMoney()).subtract(userMoneyLog.getUseMoney()).longValue());
            userMapper.updateByPrimaryKey(tradeUser);
        }
        //4.回退余额...
        if(userMoneyLog.getMoneyLogType().intValue()==ShopCode.SHOP_USER_MONEY_REFUND.getCode().intValue()){
            if(r<0){
                //如果没有支付,则不能回退余额
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY);
            }
            //防止多次退款
            TradeUserMoneyLogExample userMoneyLogExample2 = new TradeUserMoneyLogExample();
            TradeUserMoneyLogExample.Criteria criteria1 = userMoneyLogExample2.createCriteria();
            //构建查询条件
            criteria1.andOrderIdEqualTo(userMoneyLog.getOrderId());
            criteria1.andUserIdEqualTo(userMoneyLog.getUserId());
            criteria1.andMoneyLogTypeEqualTo(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
            int r2 = userMoneyLogMapper.countByExample(userMoneyLogExample2);
            if(r2>0){
                //已退款，不需要重复退款
                CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_ALREADY);
            }
            //退款
            tradeUser.setUserMoney(new BigDecimal(tradeUser.getUserMoney()).add(userMoneyLog.getUseMoney()).longValue());
            userMapper.updateByPrimaryKey(tradeUser);
        }
        //5.记录订单余额使用日志
        userMoneyLog.setCreateTime(new Date());
        userMoneyLogMapper.insert(userMoneyLog);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
