package com.example.shop.common.exception;

import com.example.shop.common.constant.ShopCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CastException {

    public static void cast(ShopCode shopCode) {
        log.info(shopCode.toString());
        throw new CustomerException(shopCode);
    }

}
