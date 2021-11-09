package com.itheima.restkeeper.constant;

/**
 * @ClassName TardingConstant.java
 * @Description TODO
 */
public class TradingConstant {



    //【阿里云退款返回状态】
    //REFUND_SUCCESS:成功
    public static final String REFUND_SUCCESS= "REFUND_SUCCESS";

    //【阿里云返回付款状态】
    //TRADE_CLOSED:未付款交易超时关闭，或支付完成后全额退款
    public static final String ALI_TRADE_CLOSED ="TRADE_CLOSED";
    //TRADE_SUCCESS:交易支付成功
    public static final String ALI_TRADE_SUCCESS="TRADE_SUCCESS";
    //TRADE_FINISHED:交易结束不可退款
    public static final String ALI_TRADE_FINISHED ="TRADE_FINISHED";

    //【平台:交易渠道】
    //阿里支付
    public static final String TRADING_CHANNEL_ALI_PAY = "ALI_PAY";
    //微信支付
    public static final String TRADING_CHANNEL_WECHAT_PAY = "WECHAT_PAY";
    //现金
    public static final String TRADING_CHANNEL_CASHPAY = "CASHPAY";
    //免单挂账
    public static final String TRADING_CHANNEL_CREDIT_CARD = "CREDIT_CARD";

    //【平台:交易类型】
    //付款
    public static final String TRADING_TYPE_FK = "FK";
    //退款
    public static final String TRADING_TYPE_TK = "TK";
    //免单
    public static final String TRADING_TYPE_MD = "MD";
    //挂账
    public static final String TRADING_TYPE_GZ = "GZ";

    //【平台:交易单、订单状态】
    //待付款
    public static final String DFK ="DFK";
    //付款中
    public static final String FKZ ="FKZ";
    //已结算
    public static final String YJS ="YJS";
    //取消订单
    public static final String QXDD ="QXDD";
    //免单
    public static final String MD ="MD";
    //挂账
    public static final String GZ ="GZ";

    //【平台：退款状态】
    //成功
    public static final String REFUND_STATUS_SUCCESS = "SUCCESS";
    //请求中
    public static final String REFUND_STATUS_SENDING= "SENDING";

}