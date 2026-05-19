package com.socialmanager.service;

import com.socialmanager.model.ImageGeneration;
import com.socialmanager.model.User;
import com.socialmanager.repository.ImageGenerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenService {

    @Value("${app.leonardo.api-key:}")
    private String apiKey;

    @Value("${app.leonardo.url:}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // Inject Repository vào để lưu DB
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

        Map<String, Object> requestBody = Map.of(
            "prompt", prompt,
            "width", 1024,
            "height", 1024,
            "num_images", 1
        );

        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(requestBody, headers), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> sdGenerationJob = (Map<String, Object>) response.getBody().get("sdGenerationJob");
            String generationId = (String) sdGenerationJob.get("generationId");
            
            log.info("✅ THÀNH CÔNG! GenerationId: {}", generationId);
            return generationId;
        } catch (Exception e) {
            log.error("❌ LỖI GỌI LEONARDO: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi gọi Leonardo API: " + e.getMessage(), e);
        }
    }

    //Hàm lưu yêu cầu vào Database
    public ImageGeneration startImageGeneration(String prompt, User user) {
        // Gọi hàm số 1 để lấy ID từ Leonardo (throws exception if fails)
        String genId = generateImageRequest(prompt);

        // Tạo Entity và lưu vào DB với trạng thái PENDING
        ImageGeneration imgGen = new ImageGeneration();
        imgGen.setPrompt(prompt);
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