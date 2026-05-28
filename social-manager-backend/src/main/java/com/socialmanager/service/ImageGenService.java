package com.socialmanager.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.socialmanager.model.ImageGeneration;
import com.socialmanager.model.User;
import com.socialmanager.repository.ImageGenerationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenService {

    @Value("${app.leonardo.api-key}")
    private String apiKey;

    @Value("${app.leonardo.url}")
    private String apiUrl;

    @Value("${app.leonardo.webhook-enabled:false}")
    private boolean webhookEnabled;

    @Value("${app.leonardo.webhook-url:}")
    private String webhookUrl;

    @Value("${app.leonardo.default-negative-prompt:low quality, blurry, pixelated, text, watermark, logo, deformed face, bad anatomy, extra fingers, extra limbs}")
    private String defaultNegativePrompt;

    @Value("${app.leonardo.default-guidance-scale:8}")
    private int defaultGuidanceScale;

    @Value("${app.leonardo.default-num-inference-steps:30}")
    private int defaultNumInferenceSteps;

    @Value("${app.leonardo.default-model-id:}")
    private String defaultModelId;

    private final ImageGenerationRepository imageGenerationRepository;

    // 1. Hàm gửi yêu cầu sang Leonardo (Lấy ID)
    public String generateImageRequest(String prompt) {
        log.info(">>>> ĐANG GỬI YÊU CẦU TẠO ẢNH SANG LEONARDO...");

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-leonardo-api-key-here")) {
            log.error("❌ LEONARDO API KEY CHƯA ĐƯỢC CẤU HÌNH!");
            throw new RuntimeException("Leonardo API key chưa được cấu hình. Vui lòng thêm app.leonardo.api-key vào .env");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String enrichedPrompt = buildPrompt(prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", enrichedPrompt);
        requestBody.put("width", 1024);
        requestBody.put("height", 1024);
        requestBody.put("num_images", 1);
        requestBody.put("negative_prompt", defaultNegativePrompt);
        requestBody.put("guidance_scale", defaultGuidanceScale);
        requestBody.put("num_inference_steps", defaultNumInferenceSteps);

        if (defaultModelId != null && !defaultModelId.isBlank()) {
            requestBody.put("modelId", defaultModelId);
        }

        // Tạm thời KHÔNG gửi webhook để tránh Leonardo 500:
        // {"error":"not a valid json response from webhook","code":"unexpected"}.
        // Pipeline async sẽ do worker polling xử lý.

        log.info(">>>> Leonardo request payload keys: {}", requestBody.keySet());
        log.info(">>>> Leonardo prompt (trimmed): {}", enrichedPrompt.substring(0, Math.min(220, enrichedPrompt.length())));

        RestTemplate restTemplate = new RestTemplate();

        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(requestBody, headers), Map.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Leonardo trả về body rỗng");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> sdGenerationJob = (Map<String, Object>) response.getBody().get("sdGenerationJob");

            if (sdGenerationJob == null || sdGenerationJob.get("generationId") == null) {
                throw new RuntimeException("Leonardo response không có generationId: " + response.getBody());
            }

            String generationId = (String) sdGenerationJob.get("generationId");
            log.info("✅ THÀNH CÔNG! GenerationId: {}", generationId);
            return generationId;
        } catch (HttpStatusCodeException e) {
            log.error("❌ LỖI GỌI LEONARDO [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Lỗi Leonardo API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("❌ LỖI GỌI LEONARDO: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi gọi Leonardo API: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String rawPrompt) {
        String cleaned = rawPrompt == null ? "" : rawPrompt.trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            cleaned = "Lifestyle social media visual";
        }

        return cleaned
            + ". Ultra-detailed, visually appealing social media image, strong subject clarity, balanced composition, cinematic lighting, natural colors, sharp focus, high contrast, 4k quality, realistic textures, professional photography.";
    }

    // Hàm lưu yêu cầu vào Database
    public ImageGeneration startImageGeneration(String prompt, User user) {
        return startImageGeneration(prompt, null, user);
    }

    public ImageGeneration startImageGeneration(String prompt, String caption, User user) {
        // Gọi hàm số 1 để lấy ID từ Leonardo (throws exception if fails)
        String genId = generateImageRequest(prompt);

        // Tạo Entity và lưu vào DB với trạng thái PENDING
        ImageGeneration imgGen = new ImageGeneration();
        imgGen.setPrompt(prompt);
        if (caption != null && !caption.isBlank()) {
            imgGen.setCaption(caption);
        }
        imgGen.setLeonardoGenerationId(genId);
        imgGen.setStatus("PENDING");
        imgGen.setUser(user);

        return imageGenerationRepository.save(imgGen);
    }

    // Hàm kiểm tra trạng thái ảnh (Dùng cho Scheduler)
    public String getImageUrl(String generationId) {
        log.info(">>>> ĐANG KIỂM TRA TRẠNG THÁI ẢNH CHO ID: {}", generationId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            String checkUrl = apiUrl + "/" + generationId;
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(checkUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();

            @SuppressWarnings("unchecked")
            Map<String, Object> generationsByPk = (Map<String, Object>) body.get("generations_by_pk");
            String status = (String) generationsByPk.get("status");

            if ("COMPLETE".equals(status)) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> images = (java.util.List<Map<String, Object>>) generationsByPk.get("generated_images");
                if (images != null && !images.isEmpty()) {
                    return (String) images.get(0).get("url");
                }
            }
            return "PENDING";
        } catch (Exception e) {
            log.error("❌ LỖI KHI CHECK ẢNH: {}", e.getMessage());
            return "ERROR";
        }
    }
}
