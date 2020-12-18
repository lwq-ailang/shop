package com.example.api;


import com.example.shop.entity.Result;
import com.example.shop.pojo.TradeCoupon;

/**
 * 优惠券接口
 */
public interface ICouponService {

    /**
     * 根据ID查询优惠券对象
     * @param coupouId
     */
    public TradeCoupon findOne(Long coupouId);

    /**
     * 更细优惠券状态
     * @param coupon
     */
    Result updateCouponStatus(TradeCoupon coupon);
}
