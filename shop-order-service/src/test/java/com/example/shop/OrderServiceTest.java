package com.example.shop;

import com.example.api.IOrderService;
import com.example.shop.common.constant.ShopCode;
import com.example.shop.common.exception.CustomerException;
import com.example.shop.entity.Result;
import com.example.shop.pojo.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigDecimal;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderServiceApplication.class)
@Slf4j
public class OrderServiceTest {

    @Autowired
    private IOrderService orderService;

    @Test
    public void confirmOrder() throws IOException {
        Result result = new Result();
        try {
            Long coupouId = 345988230098857984L;
            Long goodsId = 345959443973935104L;
            Long userId = 345963634385633280L;

            TradeOrder order = new TradeOrder();
            order.setGoodsId(goodsId);
            order.setUserId(userId);
            order.setCouponId(coupouId);
            order.setAddress("北京"); //收货地址
            order.setGoodsNumber(1); //购买商品数量
            order.setGoodsPrice(new BigDecimal(1000)); //购买商品价格
            order.setShippingFee(BigDecimal.ZERO);//运费
            order.setOrderAmount(new BigDecimal(1000)); //订单总金额
            order.setMoneyPaid(new BigDecimal(100));//用户余额
            orderService.confirmOrder(order);
        } catch (CustomerException customerException) {
            result.setCode(customerException.getCodeByCustomerException());
            result.setMessage(customerException.getMessageByCustomerException());
            log.info("{}",result);
        }catch (Exception exception){
            exception.printStackTrace();
        }

        //保证服务不要停止
        System.in.read();
    }

}
