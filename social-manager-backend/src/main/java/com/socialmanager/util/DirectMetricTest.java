package com.socialmanager.util;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Test trực tiếp Facebook API với token đã decrypt
 * Chạy: java -cp target/classes com.socialmanager.util.DirectMetricTest
 */
public class DirectMetricTest {

    private static final String ENCRYPTED_TOKEN = "HTta4FMnqwNlzbgSTmArVdf4NYQciyV/rkaAPDIbhTNQt+0ckNOPNITp2Moo326aGMfiLx1mlrAn7LBps4wgigPk+QMO/OHxKiL67sNbORSRdMwKeXteE5Ovtc1r54F2mknjcPeohZczIvbdBmKhuqfdJBtLkOFGlz97e+uPKyqqlF4qabyomktXAF6UUAycRD8ppCz8DthZ5r+1AdpyZD+LG06ubxKMxwNcYfQCaKd9lts/Nn9443dA4oPEhRMkNBrWkv6cgwyK3ElZV6YoCY6d6jCnSVj6vP6uMXvmUX47HO5s6XgO5IKsKVE=";
    private static final String AES_SECRET = System.getProperty("AES_SECRET", System.getenv("AES_SECRET"));
    private static final String POST_ID = "656215827578121_122171530550897422";
    
    private static final List<String> METRICS = List.of(
        "post_impressions",
        "post_impressions_unique",
        "post_impressions_paid",
        "post_impressions_organic",
        "post_engaged_users",
        "post_clicks",
        "post_reactions_by_type_total"
    );

    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("🔍 TESTING POST METRICS");
        System.out.println("========================================\n");

        try {
            // Decrypt token
            String pageToken = EncryptionUtil.decrypt(ENCRYPTED_TOKEN, AES_SECRET);
            System.out.println("✅ Token decrypted successfully");
            System.out.println("🔑 Token: " + pageToken.substring(0, 30) + "...\n");

            RestTemplate restTemplate = new RestTemplate();
            int successCount = 0;
            int failCount = 0;

            for (String metric : METRICS) {
                try {
                    String url = String.format(
                        "https://graph.facebook.com/v25.0/%s/insights?metric=%s",
                        POST_ID, metric
                    );

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + pageToken);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                    );

                    System.out.println("✅ OK    " + metric);
                    successCount++;

                    Thread.sleep(300);

                } catch (HttpClientErrorException e) {
                    System.out.println("❌ FAIL  " + metric);
                    System.out.println("         → " + e.getResponseBodyAsString());
                    failCount++;
                } catch (Exception e) {
                    System.out.println("❌ ERROR " + metric);
                    System.out.println("         → " + e.getMessage());
                    failCount++;
                }
            }

            System.out.println("\n========================================");
            System.out.println("📊 SUMMARY");
            System.out.println("========================================");
            System.out.println("✅ Success: " + successCount);
            System.out.println("❌ Failed:  " + failCount);
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
