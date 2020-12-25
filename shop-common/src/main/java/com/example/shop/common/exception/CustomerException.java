package com.example.shop.common.exception;


import com.example.shop.common.constant.ShopCode;
import com.example.shop.entity.Result;

/**
 * 自定义异常
 */
public class CustomerException extends RuntimeException{

    private ShopCode shopCode;

    public CustomerException(ShopCode shopCode) {
        this.shopCode = shopCode;
    }

    public ShopCode getShopCode() {
        return shopCode;
    }

    public void setShopCode(ShopCode shopCode) {
        this.shopCode = shopCode;
    }

    public Result result(ShopCode shopCode){
        return new Result(shopCode.getSuccess(),shopCode.getMessage(),shopCode.getCode());
    }

    public int getCodeByCustomerException(){
        return shopCode.getCode();
    }

    public String getMessageByCustomerException(){
        return shopCode.getMessage();
    }
}
