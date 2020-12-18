package com.example.shop;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import com.example.shop.common.utils.IDWorker;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDubboConfiguration
@MapperScan("com.example.shop.mapper")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class,args);
    }

    //注入IDWorker
    @Bean
    public IDWorker getBean(){
        return new IDWorker(1,1);
    }

}
