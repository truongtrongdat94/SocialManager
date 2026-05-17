package com.socialmanager.service;

import com.socialmanager.model.ImageGeneration;
import com.socialmanager.model.User;
import com.socialmanager.repository.ImageGenerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
@ConditionalOnExpression("'${app.leonardo.api-key:}'.length() > 0")
public class ImageGenService {

    @Value("${app.leonardo.api-key}")
    private String apiKey;

    @Value("${app.leonardo.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // Inject Repository vào để lưu DB
    private final ImageGenerationRepository imageGenerationRepository;

    // 1. Hàm gửi yêu cầu sang Leonardo (Lấy ID)
    public String generateImageRequest(String prompt) {
        log.info(">>>> ĐANG GỬI YÊU CẦU TẠO ẢNH SANG LEONARDO...");

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
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(requestBody, headers), Map.class);
            Map sdGenerationJob = (Map) response.getBody().get("sdGenerationJob");
            String generationId = (String) sdGenerationJob.get("generationId");
            
            log.info("✅ THÀNH CÔNG! GenerationId: {}", generationId);
            return generationId;
        } catch (Exception e) {
            log.error("❌ LỖI GỌI LEONARDO: {}", e.getMessage());
            return null;
        }
    }

    //Hàm lưu yêu cầu vào Database
    public ImageGeneration startImageGeneration(String prompt, User user) {
        // Gọi hàm số 1 để lấy ID từ Leonardo
        String genId = generateImageRequest(prompt);
        
        if (genId == null) return null;

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
            ResponseEntity<Map> response = restTemplate.exchange(checkUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);
            Map body = response.getBody();

            Map generationsByPk = (Map) body.get("generations_by_pk");
            String status = (String) generationsByPk.get("status");
            
            if ("COMPLETE".equals(status)) {
                java.util.List<Map> images = (java.util.List<Map>) generationsByPk.get("generated_images");
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