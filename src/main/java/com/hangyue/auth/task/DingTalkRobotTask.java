package com.hangyue.auth.task;

/**
 * @Description TODO
 * @Author raven
 * @Date 2025/7/14 16:21
 * @Version 1.0
 **/
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Component
public class DingTalkRobotTask {

    // 你的Webhook地址
    private static final String WEBHOOK = "https://oapi.dingtalk.com/robot/send?access_token=af41aa545e363e3bcdfdae8fe3101fd549d9234a70e6ea81fafc809295e3ebde";
    // 如果开启了加签，填写secret，否则留空
    private static final String SECRET = "SEC6e40c49e5fd5ffc360e80e701c725861c627d7c79f42507f51c631a323264278";

    //每5秒执行一次
//    @Scheduled(cron = "0/5 * * * * ?")
//    @Scheduled(cron = "0 */1 * * * ?") // 每天9点发送
    public void sendMsg() throws Exception {
        String url = WEBHOOK;
        if (SECRET != null && !SECRET.isEmpty()) {
            long timestamp = System.currentTimeMillis();
            String sign = DingTalkSignUtil.getSign(timestamp, SECRET);
            url = url + "&timestamp=" + timestamp + "&sign=" + sign;
        }

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> msg = new HashMap<>();
        msg.put("msgtype", "text");
        Map<String, String> text = new HashMap<>();
        text.put("content", "下班了没，定时提醒消息！");
        msg.put("text", text);

        restTemplate.postForObject(url, msg, String.class);
    }
}