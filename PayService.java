package com.pinshang.qingyun.xsorder.service;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import com.pinshang.qingyun.base.enums.*;
import com.pinshang.qingyun.box.utils.JsonUtil;
import com.pinshang.qingyun.kafka.KafkaConstant;
import com.pinshang.qingyun.kafka.MessageOperationType;
import com.pinshang.qingyun.kafka.MessageType;
import com.pinshang.qingyun.kafka.MessageWrapper;
import com.pinshang.qingyun.shop.dto.ShopSettingODTO;
import com.pinshang.qingyun.shop.service.ShopSettingClient;
import com.pinshang.qingyun.xsorder.conf.AlipayConfig;
import com.pinshang.qingyun.xsorder.conf.MyWeChatPayConfig;
import com.pinshang.qingyun.xsorder.mapper.PayBillMapper;
import com.pinshang.qingyun.xsorder.mapper.XSReturnOrderLogMapper;
import com.pinshang.qingyun.xsorder.mapper.XSReturnOrderMapper;
import com.pinshang.qingyun.xsorder.mapper.XsOrderMapper;
import com.pinshang.qingyun.xsorder.model.PayBill;
import com.pinshang.qingyun.xsorder.model.XsOrder;
import com.pinshang.qingyun.xsorder.model.returnOrder.XSReturnOrder;
import com.pinshang.qingyun.xsorder.model.returnOrder.XSReturnOrderLog;
import com.pinshang.qingyun.xsorder.vo.returnOrder.PayReturnOrderVO;
import com.pinshang.qingyun.xsorder.vo.returnOrder.WxPayReturnOrderVo;
import com.pinshang.qingyun.xsorder.vo.returnOrder.WxPayReturnValueVo;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by honway on 2017/8/15 11:27.
 * 支付服务
 */
@Service
@Slf4j
public class PayService {


    private MyWeChatPayConfig weChatPayConfig;

    private AlipayConfig alipayConfig;

    private XsOrderMapper orderMapper;

    private ShopSettingClient shopSettingClient;

    private AlipayClient alipayClient;

    private WXPay wxpay;

    private PayBillMapper payBillMapper;

    private XSReturnOrderMapper returnOrderMapper;

    private XSReturnOrderLogMapper returnOrderLogMapper;

    private static final String WECHAT_PAY_FAIL = "<xml>\n" +
            "  <return_code><![CDATA[" + WXPayConstants.FAIL + "]]></return_code>\n" +
            "  <return_msg><![CDATA[签名错误]]></return_msg>\n" +
            "</xml>";
    private static final String WECHAT_PAY_SUCCESS = "<xml>\n" +
            "  <return_code><![CDATA[" + WXPayConstants.SUCCESS + "]]></return_code>\n" +
            "  <return_msg><![CDATA[OK]]></return_msg>\n" +
            "</xml>";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public PayService(MyWeChatPayConfig weChatPayConfig, AlipayConfig alipayConfig, XsOrderMapper orderMapper, ShopSettingClient shopSettingClient, PayBillMapper payBillMapper, XSReturnOrderMapper returnOrderMapper, XSReturnOrderLogMapper returnOrderLogMapper) {
        this.weChatPayConfig = weChatPayConfig;
        this.alipayConfig = alipayConfig;
        this.orderMapper = orderMapper;
        this.shopSettingClient = shopSettingClient;
        this.payBillMapper = payBillMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.returnOrderLogMapper = returnOrderLogMapper;
        String gateway = alipayConfig.getGateway();
        String appid = alipayConfig.getAppid();
        String appPrivateKey = alipayConfig.getAppPrivateKey();
        String charset = alipayConfig.getCharset();
        String dataFormat = alipayConfig.getDataFormat();
        String alipayPublicKey = alipayConfig.getAlipayPublicKey();
        String encodeType = alipayConfig.getEncodeType();
        this.alipayClient = new DefaultAlipayClient(gateway, appid, appPrivateKey, dataFormat, charset, alipayPublicKey, encodeType);
        this.wxpay = new WXPay(weChatPayConfig, WXPayConstants.SignType.MD5);

    }

    /**
     * 生成微信预支付订单
     * 1. 请求微信接口,生成预支付订单
     * 2. 拿到微信返回的预付订单id(prepayid), 再结合APP端调用微信SDK需要的另外几个参数, 重新生成签名,返回给APP端
     *
     * @param orderCode 订单号
     * @param userId 用户ID
     * @return 返回 APP端需要调用微信SDK的所有信息
     */
    @Transactional
    public Map<String, String> weChatPay(Long orderCode, Long userId, String clientIP) {
        XsOrder order = checkAndReturnOrder(orderCode, userId);
        clientIP = proceedClientIP(clientIP);
        Map<String, String> data = new HashMap<>();
        data.put("body", "清美鲜食");
        data.put("out_trade_no", order.getOrderCode().toString());
        data.put("fee_type", "CNY");
        //只在开发测试阶段, 订单金额永远为0.01元,上线前要改掉
        data.put("total_fee", order.getPayAmount().multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString());
        //data.put("total_fee", "1");
        data.put("spbill_create_ip", clientIP);
        data.put("notify_url", weChatPayConfig.getCallbackUrl());
        data.put("trade_type", "APP");  // 此处指定为APP支付
        Map<String, String> unifiedOrder;
        try {
            unifiedOrder = wxpay.unifiedOrder(data);
            Assert.notNull(unifiedOrder, "网络故障.");
            Assert.isTrue(WXPayConstants.SUCCESS.equalsIgnoreCase(unifiedOrder.get("return_code")), unifiedOrder.get("return_msg"));
            String prepayId = unifiedOrder.get("prepay_id");
            Assert.hasLength(prepayId, "生成预付订单失败.");

            // 是不是要更新pay bill, 如果之前选择了支付宝付款
            // 后面没有支付又切换为微信, pay bill里会存在支付宝的付款流水
            // 要更新为微信的
            boolean updatePayBillFlag = false;
            if (order.getPayType() != null) {
                updatePayBillFlag = true;
            }

            // 更新订单支付类型为微信
            order.setPayType(XSPayTypeEnums.WECHAT.getCode());
            order.setUpdateTime(new Date());
            orderMapper.updateByPrimaryKeySelective(order);

            // 记录流水
            proceedPayBill(orderCode, userId, order, updatePayBillFlag);
            try {
                Map<String, String> result = new HashMap<>();
                result.put("appid", weChatPayConfig.getAppID());
                result.put("partnerid", weChatPayConfig.getMchID());
                result.put("prepayid", unifiedOrder.get("prepay_id"));
                result.put("package", weChatPayConfig.getPackages());
                result.put("noncestr", WXPayUtil.generateNonceStr());
                result.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
                String signature = WXPayUtil.generateSignature(result, weChatPayConfig.getKey());
                result.put("sign", signature);
                return result;
            } catch (Exception e) {
                log.error("生成微信签名时异常: {}", e);
            }
        } catch (Exception e) {
            log.error("调用微信接口生成预付订单时异常: {}", e);

        }
        return null;
    }

    /**
     * 记录订单支付流水信息
     * 如果之前选择了微信付款,流水里面会记录一条微信的付款记录
     * 但是后面切换成了支付宝付款, 需要更新这条记录. 因为同一个单只会有一个流水.唯一索引限制
     * @param orderCode 订单号
     * @param userId 用户ID
     * @param order 订单
     * @param updateFlag 是不是要更新状态
     */
    private void proceedPayBill(Long orderCode, Long userId, XsOrder order, boolean updateFlag) {
        if (updateFlag) {
            Example example = new Example(PayBill.class);
            example.createCriteria().andEqualTo("billCode", orderCode).andEqualTo("billStatus", XSPayBillStatusEnums.WAITING_PAY.getCode());
            List<PayBill> payBillList = payBillMapper.selectByExample(example);
            if (payBillList != null && payBillList.size() == 1) {
                PayBill payBill = payBillList.get(0);
                payBill.setPayType(order.getPayType());
                payBillMapper.updateByPrimaryKeySelective(payBill);
            }
            return;
        }
        PayBill payBill = new PayBill();
        payBill.setBillCode(orderCode);
        payBill.setBillStatus(XSPayBillStatusEnums.WAITING_PAY.getCode());
        payBill.setReferType(XSPayBillEnums.PAY.getCode());
        payBill.setPayType(order.getPayType());
        payBill.setCreateId(userId);
        payBill.setUpdateId(userId);
        payBill.setCreateTime(new Date());
        payBill.setUpdateTime(new Date());
        payBill.setPayAmount(order.getPayAmount());
        payBillMapper.insert(payBill);
    }

    /**
     * 获取沙箱签名
     *
     * 写好后就没有使用过,甚用!!!
     *
     * @param wxPay 微信支付工具类
     * @return 返回微信沙箱环境返回的沙箱签名, 可以用这个签名, 代替 用户的key
     * @throws Exception 获取沙箱签名失败时的异常
     */
    @Deprecated
    private Map<String, String> getSandboxSign(WXPay wxPay) throws Exception {
        Map<String, String> reqData = new HashMap<>();
        reqData.put("mch_id", weChatPayConfig.getMchID());
        reqData.put("nonce_str", WXPayUtil.generateNonceStr());
        String signature = WXPayUtil.generateSignature(reqData, weChatPayConfig.getKey(), WXPayConstants.SignType.MD5);
        reqData.put("sign", signature);
        String s = wxPay.requestWithoutCert("https://api.mch.weixin.qq.com/sandboxnew/pay/getsignkey", reqData, 4000, 10000);
        System.out.println(s);
        Map<String, String> sandBoxResultMap = WXPayUtil.xmlToMap(s);
        Assert.isTrue(WXPayConstants.SUCCESS.equalsIgnoreCase(sandBoxResultMap.get("return_code")), "生成沙箱环境签名时错误");
        return sandBoxResultMap;
    }

    /**
     * 检查订单是否有效
     * @param orderCode 订单号
     * @param userId 用户ID
     * @return 返回有效的订单
     */
    private XsOrder checkAndReturnOrder(Long orderCode, Long userId) {
        Assert.isTrue(!(orderCode == null || userId == null), "订单号或用户ID不能为空.");

        Example example = new Example(XsOrder.class);
        example.createCriteria().andEqualTo("orderCode", orderCode).andEqualTo("userId", userId).andEqualTo("orderStatus", XSOrderStatusEnums.WAITING_PAY.getCode());
        List<XsOrder> xsOrders = orderMapper.selectByExample(example);
        Assert.notNull(xsOrders, "找不到该订单信息,订单号: " + orderCode);
        Assert.isTrue(xsOrders.size() == 1, "找不到该订单信息,订单号: " + orderCode);

        return xsOrders.get(0);
    }

    /**
     * 微信回调
     * @param resVo 回调参数列表
     *              return_code SUCCESS/FAIL 此字段是通信标识，非交易标识，交易是否成功需要查看result_code来判断
     *              return_msg  返回信息，如非空，为错误原因(签名失败 参数格式校验错误)
     * @return 返回给微信处理成功 或 失败
     */
    public String weChatCallback(String resVo) {
        Map<String, String> notifyMap;  // 转换成map
        try {
            notifyMap = WXPayUtil.xmlToMap(resVo);
            if (wxpay.isPayResultNotifySignatureValid(notifyMap)) {
                // 签名正确
                // 进行处理.
                // 注意特殊情况:订单已经退款，但收到了支付结果成功的通知,不应把商户侧订单状态从退款改成支付成功
                String returnCode = notifyMap.get("return_code");
                if (WXPayConstants.SUCCESS.equalsIgnoreCase(returnCode)) {
                    if (WXPayConstants.SUCCESS.equalsIgnoreCase(notifyMap.get("result_code"))) {
                        // 支付成功
                        String orderCode = notifyMap.get("out_trade_no");
                        // 有可是重复的回调,已经修改过状态. 这样会导致xsOrder为空
                        XsOrder xsOrder = proceedCallbackOrder(orderCode);
                        if (xsOrder != null) {
                            // 微信支付订单号(流水号)
                            String transactionId = notifyMap.get("transaction_id");
                            proceedPayBillCallback(xsOrder, transactionId, XSPayBillStatusEnums.WAITING_PAY);
                        }
                    }
                }
            }
            else {
                // 签名错误，如果数据里没有sign字段，也认为是签名错误
                log.error("微信支付回调签名错误, 数据可能被篡改: {}", JSON. toJSONString(notifyMap));
                return WECHAT_PAY_FAIL;
            }
        }catch (Exception e) {
            log.error("处理微信回调失败:{}", e);
            return WECHAT_PAY_FAIL;
        }

        return WECHAT_PAY_SUCCESS;
    }

    /**
     * 回调更新支付状态
     * @param xsOrder 订单
     * @param transactionId 第三方支付流水号
     * @param payBillEnums 流水状态
     */
    private void proceedPayBillCallback(XsOrder xsOrder, String transactionId, XSPayBillStatusEnums payBillEnums) {
        if (xsOrder == null) {
            return;
        }

        Example example = new Example(PayBill.class);
        example.createCriteria().andEqualTo("billCode", xsOrder.getOrderCode()).andEqualTo("referType", payBillEnums.getCode());
        List<PayBill> payBills = payBillMapper.selectByExample(example);
        if (payBills != null && payBills.size() > 0) {
            PayBill payBill = payBills.get(0);
            payBill.setTradeBillCode(transactionId);
            payBill.setBillStatus(payBillEnums.getCode());
            payBillMapper.updateByPrimaryKeySelective(payBill);


            try{
                //发送消息，通知有新的订单
                String uuid = UUID.randomUUID().toString();
                MessageWrapper message = new MessageWrapper(MessageType.XS_ORDER_BUY, xsOrder.getShopId().toString(), MessageOperationType.SEND_SHOP_ORDER, uuid);
                kafkaTemplate.send(KafkaConstant.XS_SHOP_ORDER_TOPIC, JSON.toJSONString(message));
            }catch (Exception ex){
                log.error("支付发送消息,出现异常", ex.getMessage());
            }

        } else {
            log.error("查询不到该订单的支付流水:{}", xsOrder.getOrderCode());
        }

    }

    /**
     * 处理回调, 订单支付状态修改
     * @param orderCode 订单号
     */
    private XsOrder proceedCallbackOrder(String orderCode) {
        if (StringUtils.isNotBlank(orderCode)) {
            Example example = new Example(XsOrder.class);
            // 根据订单号查询 待付款的订单
            example.createCriteria().andEqualTo("orderCode", orderCode).andEqualTo("payStatus", XSOrderPayStatusEnums.WAITING_PAY.getCode());
            List<XsOrder> xsOrders = orderMapper.selectByExample(example);
            if (xsOrders.size() == 1) {
                // 如果找到一单, 修改状态
                XsOrder xsOrder = xsOrders.get(0);
                // 修改订单支付状态为2, 已支付
                xsOrder.setPayStatus(XSOrderPayStatusEnums.PAYED.getCode());
                xsOrder.setOrderStatus(XSOrderStatusEnums.WAITING_DELIVERY.getCode());
                // 修改订单状态的时候,根据支付状态修改,确认只修改了指定单号并且是未支付状态的订单为已支付,避免并发修改问题
                Example updateExample = new Example(XsOrder.class);
                updateExample.createCriteria().andEqualTo("orderCode", orderCode).andEqualTo("payStatus", XSOrderPayStatusEnums.WAITING_PAY.getCode());
                orderMapper.updateByExampleSelective(xsOrder, updateExample);
                return xsOrder;
            }
        }
        return null;
    }

    /**
     * 支付宝支付
     * 1. 调用支付宝接口,生成预付订单
     * 2. 把支付宝返回的信息,直接返回给APP,不需要做处理,前端就可以用返回值调起支付宝APP进行支付
     * @param orderCode 订单号
     * @param userId 用户ID
     * @return 调起支付宝APP需要的信息
     */
    public String alipayPay(Long orderCode, Long userId) {
        //实例化客户端
        XsOrder order = checkAndReturnOrder(orderCode, userId);

        String callback = alipayConfig.getCallbackUrl();
        String payTimeout = "60m";
        ShopSettingODTO shopSetting = shopSettingClient.queryShopSettings();
        if (shopSetting != null) {
            // 支付宝付款超时时间 默认后台设计的订单超时时间 * 2
            payTimeout = (shopSetting.getTimeoutMinutes() << 1) + "m";
        }
        //实例化具体API对应的request类,类名称和接口名称对应,当前调用接口名称：alipay.trade.app.pay
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        //SDK已经封装掉了公共参数，这里只需要传入业务参数。以下方法为sdk的model入参方式(model和biz_content同时存在的情况下取biz_content)。
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setBody("清美鲜食");
        model.setSubject("清美鲜食");
        model.setOutTradeNo(orderCode + "");
        model.setTimeoutExpress(payTimeout);
        //只在开发测试阶段, 订单支付为0.01元,上线前要改掉
        model.setTotalAmount(order.getPayAmount().toPlainString());
        //model.setTotalAmount("0.01");
        request.setBizModel(model);
        request.setNotifyUrl(callback);
        try {
            boolean updateFlag = false;
            // 有支付方式,并且不是支付宝, 需要更新支付流水的支付方式为支付宝
            if (order.getPayType() != null) {
                updateFlag = true;
            }
            //这里和普通的接口调用不同,使用的是sdkExecute
            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
            // 更新订单支付方式为支付宝.
            order.setPayType(XSPayTypeEnums.ALIPAY.getCode());
            order.setUpdateTime(new Date());
            orderMapper.updateByPrimaryKeySelective(order);
            // 记录流水
            proceedPayBill(orderCode, userId, order, updateFlag);
            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("调用支付宝下单时异常:{}", e.getMessage());
        }
        return null;
    }

    /**
     * 微信支付需要传客户端IP地址
     * @param clientIP 客户端IP地址
     * @return 如果客户端IP地址合法,就传给获取到的IP地址,如果不合法,随便传一个固定的地址
     *          微信支付IP地址为必传项, 有时候获取客户端IP的时候会获取到IPV6, 但是微信只支持IPV4
     */
    private String proceedClientIP(String clientIP) {
        String reg = "((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3})";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(clientIP);
        boolean flag = false;
        if(matcher.find()) {
            clientIP = matcher.group(1);
            flag = true;
        }
        if (!flag) {
            clientIP = "114.114.114.114";
        }
        return clientIP;
    }

    /**
     * 支付宝回调
     * https://docs.open.alipay.com/59/103666
     *
     * @param params 支付宝回调数据.
     * @return success
     */
    public String alipayCallback(Map<String,String> params) {
        try {
            String alipayPublicKey = alipayConfig.getAlipayPublicKey();
            String charset = alipayConfig.getCharset();
            // 加号变空格处理
            String sign = params.get("sign").replaceAll(" ", "+");
            params.put("sign", sign);
            //切记alipaypublickey是支付宝的公钥，请去open.alipay.com对应应用下查看。
            boolean flag = AlipaySignature.rsaCheckV1(params, alipayPublicKey, charset, alipayConfig.getEncodeType());
            if (flag) {
                //签名正确,处理
                String tradeStatus = params.get("trade_status");
                // 交易成功处理
                // 状态TRADE_SUCCESS的通知触发条件是商户签约的产品支持退款功能的前提下，买家付款成功
                // 交易状态TRADE_FINISHED的通知触发条件是商户签约的产品不支持退款功能的前提下,买家付款成功
                // 或者,商户签约的产品支持退款功能的前提下，交易已经成功并且已经超过可退款期限。
                if (AlipayStatusEnums.TRADE_SUCCESS.name().equalsIgnoreCase(tradeStatus) || AlipayStatusEnums.TRADE_FINISHED.name().equalsIgnoreCase(tradeStatus)) {
                    String orderCode = params.get("out_trade_no");
                    XsOrder xsOrder = proceedCallbackOrder(orderCode);
                    proceedPayBillCallback(xsOrder, params.get("trade_no"), XSPayBillStatusEnums.PAY_FINISHED);
                }
            } else {
                // 签名不正确,处理
                log.error("支付宝回调时签名错误,可能数据已经被篡改:{}", JSON.toJSONString(params));
                return "failure";
            }
        } catch (AlipayApiException e) {
            log.error("回调检验签名时异常:{}", e.getMessage());
            return "failure";
        }
        return "success";
    }

    /**
     * 根据订单号查询订单状态,是否已经支付
     * @param orderCode 订单号
     * @param userId 用户ID
     * @return 返回是否已经支付
     *  返回true表示支付成功, false表示支付失败.
     */
    public String queryOrderStatus(String orderCode, Long userId) {
        Assert.isTrue(!(StringUtils.isBlank(orderCode) || userId == null), "订单号或用户ID不能为空.");

        Example example = new Example(XsOrder.class);
        example.createCriteria().andEqualTo("orderCode", orderCode).andEqualTo("userId", userId);
        List<XsOrder> xsOrders = orderMapper.selectByExample(example);
        Assert.notNull(xsOrders, "找不到该订单信息,订单号: " + orderCode);
        Assert.isTrue(xsOrders.size() == 1, "找不到该订单信息,订单号: " + orderCode);

        XsOrder order = xsOrders.get(0);
        if (order.getPayStatus().equals(XSOrderPayStatusEnums.PAYED.getCode())) {
            // 已经付款
            return "true";
        }
        // 查看支付类型是支付宝还是微信
        if (order.getPayType().equals(XSPayTypeEnums.ALIPAY.getCode())) {
            // 支付宝支付
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel queryModel = new AlipayTradeQueryModel();
            queryModel.setOutTradeNo(orderCode);
            request.setBizModel(queryModel);
            try {
                AlipayTradeQueryResponse response = alipayClient.execute(request);
                Assert.isTrue(StringUtils.isBlank(response.getSubCode()), "查询失败: " + response.getSubMsg());
                /**
                 * 交易状态：
                 * WAIT_BUYER_PAY（交易创建，等待买家付款）
                 * TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）
                 * TRADE_SUCCESS（交易支付成功）
                 * TRADE_FINISHED（交易结束，不可退款）
                 */
                if ("TRADE_SUCCESS".equalsIgnoreCase(response.getTradeStatus())) {
                    //更新订单状态
                    order.setPayStatus(XSOrderPayStatusEnums.PAYED.getCode());
                    order.setOrderStatus(XSOrderStatusEnums.WAITING_DELIVERY.getCode());
                    orderMapper.updateByPrimaryKeySelective(order);
                    // 更新订单流水支付状态
                    Example billExample = new Example(PayBill.class);
                    billExample.createCriteria().andEqualTo("billCode",orderCode)
                            .andEqualTo("payType",XSPayTypeEnums.ALIPAY.getCode())
                            .andEqualTo("billStatus",XSPayBillStatusEnums.WAITING_PAY.getCode());
                    List<PayBill> payBillList = payBillMapper.selectByExample(billExample);
                    if (payBillList != null && payBillList.size() == 1) {
                        PayBill payBill = payBillList.get(0);
                        payBill.setBillStatus(XSPayBillStatusEnums.PAY_FINISHED.getCode());
                        payBillMapper.updateByPrimaryKeySelective(payBill);
                    }
                    return "true";
                } else {
                    return "false";
                }
            } catch (AlipayApiException e) {
                log.error("调用支付宝查询订单状态时失败:{}", e.getMessage());
            }
        } else if (order.getPayType().equals(XSPayTypeEnums.WECHAT.getCode())) {
            // 微信支付
            Map<String, String> reqParam = new HashMap<>();
            reqParam.put("out_trade_no", orderCode);
            try {
                Map<String, String> resp = wxpay.orderQuery(reqParam);
                if (WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("return_code"))) {
                    // 请求成功
                    if (WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("result_code"))) {
                        // 业务成功
                        /**
                         * SUCCESS—支付成功
                         * REFUND—转入退款
                         * NOTPAY—未支付
                         * CLOSED—已关闭
                         * REVOKED—已撤销（刷卡支付）
                         * USERPAYING--用户支付中
                         * PAYERROR--支付失败(其他原因，如银行返回失败)
                         */
                        if (WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("trade_state"))) {
                            // 更新订单状态
                            order.setPayStatus(XSOrderPayStatusEnums.PAYED.getCode());
                            order.setOrderStatus(XSOrderStatusEnums.WAITING_DELIVERY.getCode());
                            orderMapper.updateByPrimaryKeySelective(order);
                            // 更新订单流水支付状态
                            Example billExample = new Example(PayBill.class);
                            billExample.createCriteria().andEqualTo("billCode",orderCode)
                                    .andEqualTo("payType",XSPayTypeEnums.WECHAT.getCode())
                                    .andEqualTo("billStatus",XSPayBillStatusEnums.WAITING_PAY.getCode());
                            List<PayBill> payBillList = payBillMapper.selectByExample(billExample);
                            if (payBillList != null && payBillList.size() == 1) {
                                PayBill payBill = payBillList.get(0);
                                payBill.setBillStatus(XSPayBillStatusEnums.PAY_FINISHED.getCode());
                                payBillMapper.updateByPrimaryKeySelective(payBill);
                            }
                            return "true";
                        } else {
                            return "false";
                        }
                    }
                } else {
                    log.error("请求查询微信订单信息时失败:{}", resp.get("return_msg"));
                }
            } catch (Exception e) {
                log.error("请求查询微信订单信息异常:{}", e);
            }
        } else {
            // 不是支付宝也不是微信,目前只有刷卡,应该是未支付
            return "false";
        }
        return "false";
    }

    /**
     * 支付宝退款
     * @param payReturnOrderVO 订单号
     * @return
     */
    public boolean alipayRefund(PayReturnOrderVO payReturnOrderVO) {
        AlipayTradeRefundResponse response = null;
        response = this.doAlipayRefund(payReturnOrderVO);
        PayBill payBill =new PayBill();
        payBill.setPayAmount(payReturnOrderVO.getRefundAmount());
        payBill.setPayType(XSPayTypeEnums.ALIPAY.getCode());
        if (payReturnOrderVO.getReferType() != null) {
            payBill.setReferType(XSPayReferTypeEnums.ORDER_CANCEL_PAY.getCode());
        } else {
            payBill.setReferType(XSPayReferTypeEnums.REFUND_PAY.getCode());
        }
        payBill.setCreateId(payReturnOrderVO.getUserId());
        payBill.setUpdateId(payReturnOrderVO.getUserId());
        if(payReturnOrderVO.getOrderItemCode() != null){
            payBill.setBillCode(payReturnOrderVO.getOrderItemCode());
        }else{
            payBill.setBillCode(payReturnOrderVO.getOrderCode());
        }
        if(response.isSuccess()){
            payBill.setBillStatus(XSPayBillStatusEnums.PAY_FINISHED.getCode());
            payBill.setTradeBillCode(response.getTradeNo());
        }else{
            payBill.setBillStatus(XSPayBillStatusEnums.PAY_FAIL.getCode());
        }
        payBillMapper.insert(payBill);
        if(response.isSuccess()){
            log.debug("支付宝退款调用成功,退款码:{}", payReturnOrderVO.getOrderCode().toString());
            return true;
        } else {
            log.error("支付宝退款调用失败,退款码:{}", payReturnOrderVO.getOrderCode().toString());
        }
        return false;
    }

    private AlipayTradeRefundResponse doAlipayRefund(PayReturnOrderVO payReturnOrderVO){
        AlipayTradeRefundResponse response = null;
        //实例化具体API对应的request类,类名称和接口名称对应,当前调用接口名称：alipay.trade.app.pay
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        //SDK已经封装掉了公共参数，这里只需要传入业务参数。以下方法为sdk的model入参方式(model和biz_content同时存在的情况下取biz_content)。
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(payReturnOrderVO.getOrderCode().toString());
        //只在开发测试阶段, 订单支付为0.01元,上线前要改掉
        model.setRefundAmount(payReturnOrderVO.getRefundAmount().toString());
//        model.setRefundAmount("0.01");
        model.setRefundReason(payReturnOrderVO.getRefundReason());
        if(payReturnOrderVO.getOrderItemCode() != null) model.setOutRequestNo(payReturnOrderVO.getOrderItemCode().toString());
        if(payReturnOrderVO.getUserId() != null) model.setOperatorId(payReturnOrderVO.getUserId().toString());
        if(payReturnOrderVO.getShopId() != null) model.setStoreId(payReturnOrderVO.getShopId().toString());
        request.setBizModel(model);
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("调用支付宝退款时异常:{}", e.getMessage());
        }
        return response;
    }

    /**微信退款
     * @param vo
     * @return
     */
    public WxPayReturnValueVo  wxPayRefund(WxPayReturnOrderVo vo){
        WxPayReturnValueVo result =new WxPayReturnValueVo();
        Map<String, String> data = new HashMap<>();
        data.put("out_trade_no", vo.getOutTradeNo().toString());
        data.put("out_refund_no", null ==vo.getOutRefundNo()? vo.getOutTradeNo().toString(): vo.getOutRefundNo().toString());
        data.put("total_fee", vo.getTotalFee().toString());
        data.put("refund_fee", vo.getRefundFee().toString());
        data.put("refund_fee_type", "CNY");
        data.put("refund_desc", vo.getRefundDesc());
        Map<String, String> returnMap =null;
        try{
            returnMap =wxpay.refund(data);
        }catch(Exception ex){
            log.error("微信退款调用异常:"+ ex.getMessage());
            log.error(null ==returnMap?"":JSONUtils.toJSONString(returnMap));
            result.setResultCode(WXPayConstants.FAIL);
            result.setErrCode(null ==returnMap?"":returnMap.get("err_code"));
            result.setErrCodeDes("系统出现异常,退款失败");
            return result;
        }
        if(null !=returnMap && !returnMap.isEmpty()){
        	log.error("微信退款返回:"+ JsonUtil.java2json(returnMap));
            result.setResultCode(returnMap.get("result_code"));
            result.setErrCode(returnMap.get("err_code"));
            result.setErrCodeDes(returnMap.get("errCodeDes"));
            result.setRefundId(returnMap.get("refundId"));
            result.setOutRefundNo(returnMap.get("outRefundNo"));
            result.setRefundFee(vo.getRefundFee());
            result.setTotalFee(vo.getTotalFee());
        }
        return result;
    }

    /**
     * 订单状态补偿, 扫描订单支付退款状态
     * @return
     */
    public boolean scanPayBill() {
        // 查询支付状态是(待支付/待退款), 付款方式是支付宝/微信的所有订单
        Example example = new Example(PayBill.class);
        example.createCriteria()
                .andIn("payType", Arrays.asList(XSPayTypeEnums.ALIPAY.getCode(), XSPayTypeEnums.WECHAT.getCode()))
                .andIn("billStatus", Arrays.asList(XSPayBillStatusEnums.WAITING_PAY.getCode(), XSPayBillStatusEnums.PAY_FAIL.getCode()));
        List<PayBill> payBills = payBillMapper.selectByExample(example);

        if (payBills == null || payBills.size() == 0) {
            return true;
        }

        List<PayBill> wechatPayBills = new ArrayList<>();

        List<PayBill> alipayPayBills = new ArrayList<>();

        payBills.forEach(item -> {
            if (item.getPayType().equals(XSPayTypeEnums.ALIPAY.getCode())) {
                alipayPayBills.add(item);
            } else {
                wechatPayBills.add(item);
            }
        });

        proceedWechatPayBill(wechatPayBills);
        proceedAlipayPayBill(alipayPayBills);
        return true;
    }

    /**
     * 处理支付宝pay bill
     * @param alipayPayBills 待处理的支付宝支付订单
     */
    private void proceedAlipayPayBill(List<PayBill> alipayPayBills) {
        // 支付 类型的订单列表
        List<PayBill> payBillList = new ArrayList<>();
        // 退款类型的订单列表
        List<PayBill> refundBillList = new ArrayList<>();
        alipayPayBills.forEach(item -> {
            if (item.getReferType().equals(XSPayBillEnums.PAY.getCode())) {
                payBillList.add(item);
            } else {
                refundBillList.add(item);
            }
        });
        // 付款订单处理
        List<PayBill> successBillCode = new ArrayList<>();
        // https://doc.open.alipay.com/docs/api.htm?spm=a219a.7629065.0.0.qZWPxZ&apiId=1049&docType=4
        for (PayBill payBill : payBillList) {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel queryModel = new AlipayTradeQueryModel();
            queryModel.setOutTradeNo(payBill.getBillCode().toString());
            queryModel.setTradeNo(payBill.getTradeBillCode());
            request.setBizModel(queryModel);
            try {
                AlipayTradeQueryResponse response = alipayClient.execute(request);
                Assert.isTrue(StringUtils.isBlank(response.getSubCode()), "查询失败: " + response.getSubMsg());
                /**
                 * 交易状态：
                 * WAIT_BUYER_PAY（交易创建，等待买家付款）
                 * TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）
                 * TRADE_SUCCESS（交易支付成功）
                 * TRADE_FINISHED（交易结束，不可退款）
                 */
                if ("TRADE_SUCCESS".equalsIgnoreCase(response.getTradeStatus())) {
                    // 支付成功
                    successBillCode.add(payBill);
                }
            } catch (AlipayApiException e) {
                log.error("调用支付宝查询订单状态时失败:{}", e);
            }
        }

        if (successBillCode.size() > 0) {
            updatePayBillAndOrderStatus(successBillCode);
        }

        // 退款订单处理
        List<PayBill> refundBillCode = new ArrayList<>();
        // https://doc.open.alipay.com/docs/api.htm?spm=a219a.7629065.0.0.qZWPxZ&apiId=1049&docType=4
        for (PayBill payBill : refundBillList) {
            if(XSPayBillStatusEnums.PAY_FAIL.getCode() == payBill.getBillStatus()){
                PayReturnOrderVO payVo =new PayReturnOrderVO();
                payVo.setOrderCode(payBill.getBillCode());
                payVo.setOrderItemCode(null);
                payVo.setRefundAmount(payBill.getPayAmount());
                payVo.setRefundReason("退款");
                AlipayTradeRefundResponse response = this.doAlipayRefund(payVo);
                if(response.isSuccess()){
                    refundBillCode.add(payBill);
                    log.debug("支付宝退款调用成功,退款码:{}", payBill.getBillCode().toString());
                }
            } else {
                ///////////////////////////////////////////////////////////////////////////
                // 商户可使用该接口查询自已通过alipay.trade.refund提交的退款请求是否执行成功
                // 该接口的返回码10000，仅代表本次查询操作成功，不代表退款成功。如果该接口返回了查询数据，则代表退款成功
                // 如果没有查询到则代表未退款成功，可以调用退款接口进行重试。重试时请务必保证退款请求号一致
                ///////////////////////////////////////////////////////////////////////////
                AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
                AlipayTradeRefundModel alipayTradeRefundModel = new AlipayTradeRefundModel();
                alipayTradeRefundModel.setOutRequestNo(payBill.getTradeBillCode());
                alipayTradeRefundModel.setOutTradeNo(payBill.getBillCode().toString());
                request.setBizModel(alipayTradeRefundModel);
                try {
                    AlipayTradeRefundResponse response = alipayClient.execute(request);
                    if (response.isSuccess()) {
                        if (StringUtils.isNotBlank(response.getRefundFee())) {
                            refundBillCode.add(payBill);
                        }
                    } else {
                        log.error("调用支付宝查询订单支付状态时失败,支付宝网关状态: {}, 支付宝业务状态{}", response.getMsg(), response.getSubMsg());
                    }
                } catch (AlipayApiException e) {
                    log.error("查询支付宝退款状态时异常:{}", e.getMessage());
                }
            }
        }
        if (refundBillCode.size() > 0) {
            updatePayBillAndReturnOrder(refundBillCode);
        }
    }

    /**
     * 处理微信pay bill
     * @param wechatPayBills 待处理的微信支付订单
     */
    private void proceedWechatPayBill(List<PayBill> wechatPayBills) {
        // 支付 类型的订单列表
        List<PayBill> payBillList = new ArrayList<>();
        // 退款类型的订单列表
        List<PayBill> refundBillList = new ArrayList<>();
        wechatPayBills.forEach(item -> {
            if (item.getReferType().equals(XSPayBillEnums.PAY.getCode())) {
                payBillList.add(item);
            } else {
                refundBillList.add(item);
            }
        });
        // 付款订单处理
        List<PayBill> successBillCode = new ArrayList<>();
        for (PayBill payBill : payBillList) {
            String tradeBillCode = payBill.getTradeBillCode();
            Map<String, String> reqParam = new HashMap<>();
            reqParam.put("transaction_id", tradeBillCode);
            try {
                Map<String, String> resp = wxpay.orderQuery(reqParam);
                if (WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("return_code"))
                        && WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("result_code"))
                        && WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("trade_state"))) {
                    /**
                     * SUCCESS—支付成功
                     * REFUND—转入退款
                     * NOTPAY—未支付
                     * CLOSED—已关闭
                     * REVOKED—已撤销（刷卡支付）
                     * USERPAYING--用户支付中
                     * PAYERROR--支付失败(其他原因，如银行返回失败)
                     */
                    successBillCode.add(payBill);
                }
            } catch (Exception e) {
                log.error("请求查询微信订单支付信息异常:{}", e.getMessage());
            }
        }

        // 把付款成功的订单, 状态改为已支付
        if (successBillCode.size() > 0) {
            updatePayBillAndOrderStatus(successBillCode);
        }

        // 退款 订单处理
        // https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=9_5&index=7
        List<PayBill> refundPayBillList = new ArrayList<>();
        for (PayBill refundPayBill : refundBillList) {
            if(XSPayBillStatusEnums.PAY_FAIL.getCode() == refundPayBill.getBillStatus()){
                BigDecimal orderAmount =null; 
                if(refundPayBill.getReferType().equals(XSPayBillEnums.CANCEL_ORDER.getCode())){
                	//订单取消退款
                	Example ex=new Example(XsOrder.class);
                	ex.createCriteria().andEqualTo("orderCode", refundPayBill.getBillCode());
                	List<XsOrder> list =orderMapper.selectByExample(ex);
                	if(null !=list && !list.isEmpty()){
                		orderAmount =list.get(0).getPayAmount();
                	}
                }else{
                	//退款
                	orderAmount =returnOrderMapper.queryAmountByReturnCode(refundPayBill.getBillCode());
                }
                log.error("refundPayBill.getBillCode():"+ refundPayBill.getBillCode() +"orderAmount:{}"+ orderAmount +",refundAmount:"+refundPayBill.getPayAmount());
                WxPayReturnOrderVo wxVo =new WxPayReturnOrderVo(refundPayBill.getBillCode(), refundPayBill.getBillCode(), orderAmount.multiply(BigDecimal.valueOf(100l)).intValue(),
                        refundPayBill.getPayAmount().multiply(BigDecimal.valueOf(100l)).intValue(), "退款");
                WxPayReturnValueVo result = this.wxPayRefund(wxVo);
                if(result.getResultCode().equalsIgnoreCase(WXPayConstants.SUCCESS)){
                    refundPayBillList.add(refundPayBill);
                }
            } else {
                Map<String, String> data = new HashMap<>();
                data.put("refund_id", refundPayBill.getTradeBillCode());
                try {
                    Map<String, String> resp = wxpay.refundQuery(data);
                    if (WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("return_code"))
                            && WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("result_code"))
                            && WXPayConstants.SUCCESS.equalsIgnoreCase(resp.get("refund_status_$n"))) {
                        /**
                         * SUCCESS—支付成功
                         * REFUND—转入退款
                         * NOTPAY—未支付
                         * CLOSED—已关闭
                         * REVOKED—已撤销（刷卡支付）
                         * USERPAYING--用户支付中
                         * PAYERROR--支付失败(其他原因，如银行返回失败)
                         */
                        refundPayBillList.add(refundPayBill);
                    }
                } catch (Exception e) {
                    //
                    log.error("请求查询微信退款信息异常:{}", e.getMessage());
                }
            }
        }
        if (refundPayBillList.size() > 0) {
            updatePayBillAndReturnOrder(refundPayBillList);
        }

    }

    /**
     * 处理退款单列表
     * @param refundPayBillList 退款单列表
     */
    @Transactional
    void updatePayBillAndReturnOrder(List<PayBill> refundPayBillList) {
        List<String> traderOutBillCode = new ArrayList<>();
        List<Long> returnOrderCode = new ArrayList<>();
        for (PayBill payBill : refundPayBillList) {
            traderOutBillCode.add(payBill.getTradeBillCode());
            if(payBill.getCreateId() != 1) returnOrderCode.add(payBill.getBillCode());
        }
        // 更新pay bill状态为已支付
        payBillMapper.updatePayBillStatus(traderOutBillCode);

        // 更新退货单状态为已退款
        returnOrderMapper.updateReturnOrderListFinish(returnOrderCode);
        // 记录退货单日志
        saveReturnLogsByReturnCodeList(returnOrderCode);
    }

    /**
     * 记录退款日志
     * @param returnOrderCode 退货单号
     */
    public void saveReturnLogsByReturnCodeList(List<Long> returnOrderCode) {
        Example example = new Example(XSReturnOrder.class);
        example.createCriteria().andIn("returnCode", returnOrderCode);
        List<XSReturnOrder> xsReturnOrders = returnOrderMapper.selectByExample(example);
        List<XSReturnOrderLog> recordList = new ArrayList<>();
        for (XSReturnOrder returnOrder : xsReturnOrders) {
            XSReturnOrderLog log = new XSReturnOrderLog();
            log.setOperateType(XSReturnOrderLogOperateTypeEnums.REFUNDED.getCode());
            log.setReturnOrderId(returnOrder.getId());
            log.setCreateName("系统");
            log.setCreateId(-1L);
            log.setCreateTime(new Date());
            recordList.add(log);
        }
        returnOrderLogMapper.insertList(recordList);
    }

    /**
     * 更新pay bill 和 订单状态为已支付
     * @param successBillCode 从微信/支付宝查询的,已经支付成功的订单
     */
    @Transactional
    void updatePayBillAndOrderStatus(List<PayBill> successBillCode) {
        List<String> traderOutBillCode = new ArrayList<>();
        List<Long> orderCode = new ArrayList<>();
        for (PayBill payBill : successBillCode) {
            traderOutBillCode.add(payBill.getTradeBillCode());
            orderCode.add(payBill.getBillCode());
        }
        // 更新pay bill状态为已支付
        payBillMapper.updatePayBillStatus(traderOutBillCode);

        // 更新订单状态为已支付
        orderMapper.batchUpdateOrderStatus(orderCode);
    }

}
