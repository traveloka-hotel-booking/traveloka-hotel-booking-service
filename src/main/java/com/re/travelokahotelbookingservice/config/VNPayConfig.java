package com.re.travelokahotelbookingservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Configuration
public class VNPayConfig {

    private static final String DEFAULT_SANDBOX_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String DEFAULT_SANDBOX_RETURN_URL = "https://traveloka-hotel-booking-app.vercel.app/payment-callback";
    private static final String DEFAULT_SANDBOX_TMN_CODE = "WBPP3F9E";
    private static final String DEFAULT_SANDBOX_HASH_SECRET = "KFGDKTFIGHOIFTEWDOFLFQBXEADGSHYC";

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url:https://traveloka-hotel-booking-app.vercel.app/payment-callback}")
    private String vnpReturnUrl;

    @Value("${vnpay.tmn-code:WBPP3F9E}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret:KFGDKTFIGHOIFTEWDOFLFQBXEADGSHYC}")
    private String vnpHashSecret;

    @Value("${vnpay.version:2.1.0}")
    private String vnpVersion;

    @Value("${vnpay.command:pay}")
    private String vnpCommand;

    public String getVnpPayUrl() {
        return (vnpPayUrl != null && !vnpPayUrl.isBlank()) ? vnpPayUrl.trim() : DEFAULT_SANDBOX_PAY_URL;
    }

    public String getVnpReturnUrl() {
        return (vnpReturnUrl != null && !vnpReturnUrl.isBlank()) ? vnpReturnUrl.trim() : DEFAULT_SANDBOX_RETURN_URL;
    }

    public String getVnpTmnCode() {
        return (vnpTmnCode != null && !vnpTmnCode.isBlank()) ? vnpTmnCode.trim() : DEFAULT_SANDBOX_TMN_CODE;
    }

    public String getVnpHashSecret() {
        return (vnpHashSecret != null && !vnpHashSecret.isBlank()) ? vnpHashSecret.trim() : DEFAULT_SANDBOX_HASH_SECRET;
    }

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return hmacSHA512(secretKey, hashData.toString());
    }
}
