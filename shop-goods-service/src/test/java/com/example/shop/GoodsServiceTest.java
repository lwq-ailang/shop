package com.example.shop;

import com.example.shop.mapper.TradeGoodsMapper;
import com.example.shop.pojo.TradeGoods;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GoodsServiceApplication.class)
public class GoodsServiceTest {

    @Autowired
    private TradeGoodsMapper mapper;

    @Test
    public void test(){
        System.out.println("start");
        TradeGoods tradeGoods = mapper.selectByPrimaryKey(345959443973935104L);
        System.out.println(tradeGoods);
    }

}