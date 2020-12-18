package com.example.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.ICouponService;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.common.exception.CastException;
import com.example.shop.entity.Result;
import com.example.shop.mapper.TradeCouponMapper;
import com.example.shop.pojo.TradeCoupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Service(interfaceClass = ICouponService.class)
public class CouponServiceImpl implements ICouponService{

    @Autowired
    private TradeCouponMapper couponMapper;

    //查询单个优惠券信息
    @Override
    public TradeCoupon findOne(Long coupouId) {
        if(coupouId==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }

        return couponMapper.selectByPrimaryKey(coupouId);
    }

    //更新优惠券状态
    @Override
    public Result updateCouponStatus(TradeCoupon coupon) {
        if(coupon==null||coupon.getCouponId()==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //更新优惠券状态
        couponMapper.updateByPrimaryKey(coupon);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
