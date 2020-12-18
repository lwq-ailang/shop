package com.example.shop.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

//用户余额日志表
public class TradeUserMoneyLog extends TradeUserMoneyLogKey implements Serializable {

    private static final long serialVersionUID = -2171246654634251878L;
    private BigDecimal useMoney;
    private Date createTime;

    public BigDecimal getUseMoney() {
        return useMoney;
    }

    public void setUseMoney(BigDecimal useMoney) {
        this.useMoney = useMoney;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}