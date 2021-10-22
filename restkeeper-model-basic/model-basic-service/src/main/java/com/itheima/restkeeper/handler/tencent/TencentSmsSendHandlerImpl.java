package com.itheima.restkeeper.handler.tencent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.restkeeper.constant.SuperConstant;
import com.itheima.restkeeper.enums.SmsSendEnum;
import com.itheima.restkeeper.exception.ProjectException;
import com.itheima.restkeeper.handler.SmsSendHandler;
import com.itheima.restkeeper.pojo.SmsChannel;
import com.itheima.restkeeper.pojo.SmsSendRecord;
import com.itheima.restkeeper.pojo.SmsTemplate;
import com.itheima.restkeeper.service.ISmsChannelService;
import com.itheima.restkeeper.service.ISmsSendRecordService;
import com.itheima.restkeeper.service.ISmsTemplateService;
import com.itheima.restkeeper.utils.EmptyUtil;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @ClassName TencentSmsSendHandlerImpl.java
 * @Description 阿里云邮件发送处理器接口
 */
@Slf4j
@Service("tencentSmsSendHandler")
public class TencentSmsSendHandlerImpl implements SmsSendHandler {

    @Autowired
    SmsClient tencentSmsConfig;

    @Autowired
    ISmsSendRecordService smsSendRecordService;

    @Autowired
    ISmsTemplateService smsTemplateService;

    @Autowired
    ISmsChannelService smsChannelService;

    @Override
    public Boolean SendSms(SmsTemplate smsTemplate,
                           SmsChannel smsChannel,
                           Set<String> mobiles,
                           LinkedHashMap<String, String> templateParam) throws Exception {
        //超过发送上限
        if (mobiles.size() > 200) {
            throw new ProjectException(SmsSendEnum.PEXCEED_THE_LIMIT);
        }
        // 实例化一个请求对象,每个接口都会对应一个request对象
        SendSmsRequest request = new SendSmsRequest();
        //接收短信的手机号码，Array of String数组格式。
        request.setPhoneNumberSet(mobiles.stream().toArray(String[]::new));
        //短信SdkAppId在短信控制台 添加应用后生成的实际 SdkAppId，
        request.setSmsSdkAppId(JSONObject.parseObject(smsChannel.getOtherConfig()).getString("smsSdkAppId"));
        //模板ID必须填写已审核通过的模板 ID
        request.setTemplateId(smsTemplate.getTemplateCode());
        //短信签名内容，使用 UTF-8 编码，必须填写已审核通过的签名
        request.setSignName(smsTemplate.getSignName());
        //模板参数，若无模板参数，则设置为空。
        if (!EmptyUtil.isNullOrEmpty(templateParam)){
            LinkedList<String> templateParamList = new LinkedList<>();
            for (Map.Entry<String,String> result:templateParam.entrySet()){
                templateParamList.add(result.getValue());
            }
            request.setTemplateParamSet(templateParamList.stream().toArray(String[]::new));
        }
        //返回的resp是一个SendSmsResponse的实例，与请求对象对应
        SendSmsResponse response = tencentSmsConfig.SendSms(request);
        SendStatus[] sendStatusSet = response.getSendStatusSet();
        String content = smsTemplate.getContent();
        for (Map.Entry<String, String> entry : templateParam.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }
        List<SmsSendRecord> sendRecords = new ArrayList<>();
        for (SendStatus sendStatusHandler : sendStatusSet) {
            String code = sendStatusHandler.getCode();
            String acceptStatus = null;
            String acceptMsg = null;
            String sendStatus = null;
            String sendMsg = null;
            if ("OK".equals(code)){
                //受理成功
                acceptStatus = SuperConstant.YES;
                acceptMsg = "受理成功";
                sendStatus = SuperConstant.SENDING;
                sendMsg = "发送中";
            }else {
                //受理失败
                acceptStatus=SuperConstant.NO;
                acceptMsg = sendStatusHandler.getMessage();
            }
            SmsSendRecord smsSendRecord = SmsSendRecord.builder()
                .sendContent(content)
                .channelLabel(smsChannel.getChannelLabel())
                .channelName(smsChannel.getChannelName())
                .acceptStatus(acceptStatus)
                .acceptMsg(acceptMsg)
                .sendStatus(sendStatus)
                .sendMsg(sendMsg)
                .mobile(sendStatusHandler.getPhoneNumber())
                .signCode(smsTemplate.getSignCode())
                .signName(smsTemplate.getSignName())
                .templateCode(smsTemplate.getTemplateCode())
                .templateId(smsTemplate.getId())
                .templateType(smsTemplate.getTemplateType())
                .serialNo(sendStatusHandler.getSerialNo())
                .templateParams(JSONObject.toJSONString(templateParam))
                .build();
            sendRecords.add(smsSendRecord);
        }
        return smsSendRecordService.saveBatch(sendRecords);
    }

    @Override
    public Boolean querySendSms(SmsSendRecord smsSendRecord) throws TencentCloudSDKException {
        // 实例化一个请求对象,每个接口都会对应一个request对象
        PullSmsSendStatusByPhoneNumberRequest request = new PullSmsSendStatusByPhoneNumberRequest();
        //拉取起始时间，UNIX 时间戳（时间：秒）。
        //注：最大可拉取当前时期前7天的数据。
        request.setBeginTime(smsSendRecord.getCreatedTime().getTime());
        //偏移量。
        //注：目前固定设置为0。
        request.setOffset(0L);
        //拉取最大条数，最多 100。
        request.setLimit(100L);
        //短信SdkAppId在短信控制台 添加应用后生成的实际 SdkAppId，
        SmsChannel smsChannel = smsChannelService.findChannelByChannelLabel(smsSendRecord.getChannelLabel());
        request.setSmsSdkAppId(JSONObject.parseObject(smsChannel.getOtherConfig()).getString("smsSdkAppId"));
        //下发目的手机号码
        request.setPhoneNumber(smsSendRecord.getMobile());
        //拉取截止时间，UNIX 时间戳（时间：秒）。
        request.setEndTime(new Date().getTime());
        // 返回的resp是一个PullSmsSendStatusByPhoneNumberResponse的实例，与请求对象对应
        PullSmsSendStatusByPhoneNumberResponse response = tencentSmsConfig.PullSmsSendStatusByPhoneNumber(request);
        PullSmsSendStatus[] pullSmsSendStatusSet = response.getPullSmsSendStatusSet();
        for (PullSmsSendStatus pullSmsSendStatus : pullSmsSendStatusSet) {
            if (pullSmsSendStatus.getSerialNo().equals(smsSendRecord.getSerialNo())){
                if ("SUCCESS".equals(pullSmsSendStatus.getReportStatus())){
                    smsSendRecord.setSendStatus(SuperConstant.YES);
                    smsSendRecord.setSendMsg("发送成功");
                    return smsSendRecordService.updateById(smsSendRecord);
                }else if ("FAIL".equals(pullSmsSendStatus.getReportStatus())){
                    smsSendRecord.setSendStatus(SuperConstant.NO);
                    smsSendRecord.setSendMsg("发送失败");
                    return smsSendRecordService.updateById(smsSendRecord);
                }else {
                    log.info("短信：{}，等待回执!",smsSendRecord.toString());
                }
                break;
            }
        }
        return true;
    }

    @Override
    public Boolean retrySendSms(SmsSendRecord smsSendRecord) throws Exception {
        //已发送，发送中的短信不处理
        if (smsSendRecord.getSendStatus().equals(SuperConstant.SENDING)||
                smsSendRecord.getSendStatus().equals(SuperConstant.YES)) {
            return true;
        }
        SmsTemplate smsTemplate = smsTemplateService.getById(smsSendRecord.getTemplateId());
        SmsChannel smsChannel = smsChannelService.findChannelByChannelLabel(smsSendRecord.getChannelLabel());
        Set<String> mobiles = new HashSet<>();
        mobiles.add(smsSendRecord.getMobile());
        LinkedHashMap<String, String> templateParam =
                JSON.parseObject(smsSendRecord.getTemplateParams(), LinkedHashMap.class);
        return SendSms(smsTemplate,smsChannel,mobiles,templateParam);
    }
}