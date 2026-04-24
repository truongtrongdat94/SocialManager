package com.socialmanager.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity; // 1. Thêm import này
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.socialmanager.model.ImageGeneration;
import com.socialmanager.model.User;
import com.socialmanager.repository.ImageGenerationRepository;
import com.socialmanager.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional 
public class GeminiAIService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.url}")
    private String apiUrl;

    private final ImageGenerationRepository repository;
    private final UserRepository userRepository; // Thêm repository này vào để tìm User
    private final RestTemplate restTemplate = new RestTemplate();

    public ImageGeneration createCaption(String topic, String platform, User currentUser) {
        log.info("Đang xử lý sinh caption cho User ID: {} với chủ đề: {}", currentUser.getId(), topic);

        // Gọi API Gemini để lấy nội dung
        String caption = callGeminiApi(topic, platform);

        // Gán currentUser (User đang đăng nhập) vào entity
        ImageGeneration generation = ImageGeneration.builder()
                .user(currentUser) 
                .prompt(topic)
                .caption(caption)
                .status("COMPLETED") // Tùy luồng của bạn
                .build();

        return repository.save(generation);
    }

    // Các hàm callGeminiApi và extractText giữ nguyên như cũ...
    private String callGeminiApi(String topic, String platform) {
        log.info(">>>> GIÁ TRỊ API KEY MÀ SPRING ĐANG ĐỌC ĐƯỢC LÀ: [{}]", apiKey);
        String prompt = String.format(
            "Bạn là một chuyên gia sáng tạo nội dung mạng xã hội. " +
            "Hãy viết DUY NHẤT 01 bài đăng chuyên nghiệp cho %s về chủ đề: %s. " +
            "Yêu cầu: Nội dung hấp dẫn, có emoji phù hợp, kèm ít nhất 5 hashtag liên quan. " +
            "Lưu ý: Chỉ trả về nội dung bài đăng, không thêm lời dẫn hay các lựa chọn khác.", 
            platform, topic
        );
        
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // Kiểm tra luôn cả URL xem nó có bị trống không
            log.info(">>>> ĐANG GỌI URL: " + apiUrl);
            
            String url = apiUrl + "?key=" + apiKey;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), Map.class);
            return extractText(response.getBody());
        } catch (Exception e) {
            log.error("Lỗi gọi Gemini: {}", e.getMessage());
            return "Nội dung đang được cập nhật...";
        }
    }

    private String extractText(Map response) {
    try {
        log.info(">>>> ĐANG BÓC TÁCH DỮ LIỆU: {}", response);
        
        List candidates = (List) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "AI không trả về kết quả.";

        Map firstCandidate = (Map) candidates.get(0);
        Map content = (Map) firstCandidate.get("content");
        List parts = (List) content.get("parts");
        
        return (String) ((Map) parts.get(0)).get("text");
    } catch (Exception e) {
        log.error("Lỗi extract: {}", e.getMessage());
        return "Lỗi xử lý nội dung từ Gemini 3.";
    }
    }
    public List<ImageGeneration> getHistoryForUser(User currentUser) {
        log.info(">>>> Đang truy xuất lịch sử cho User: {} (ID: {})", 
             currentUser.getUsername(), currentUser.getId());
             
    // Sử dụng hàm có sẵn trong repository để lọc theo ID
        return repository.findByUserId(currentUser.getId());
    }
}