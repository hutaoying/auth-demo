package com.hangyue.auth.task;

/**
 * @Description TODO
 * @Author raven
 * @Date 2025/7/14 16:22
 * @Version 1.0
 **/
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.Base64;

public class DingTalkSignUtil {
    public static String getSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
        return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
    }
}
