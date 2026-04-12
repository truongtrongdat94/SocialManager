package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.request.CaptionRequest;
import com.socialmanager.model.ImageGeneration;
import com.socialmanager.model.User;
import com.socialmanager.service.GeminiAIService;
import com.socialmanager.service.ImageGenService; 
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map; // Phải có cái này để dùng được Map

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final GeminiAIService geminiAIService;
    private final ImageGenService imageGenService; // Inject Service sinh ảnh vào đây

    @PostMapping("/generate-caption")
    public ResponseEntity<ApiResponse<ImageGeneration>> generate(@Valid @RequestBody CaptionRequest request) {
        User mockUser = new User(); 
        
        ImageGeneration result = geminiAIService.createCaption(
            request.getTopic(), 
            request.getPlatform(), 
            mockUser
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Đã sinh nội dung AI thành công", result));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(geminiAIService.getAllHistory());
    }

    // Endpoint sinh ảnh - Giữ đúng logic lấy ID từ Leonardo
    @PostMapping("/generate-image")
    public ResponseEntity<?> generateImage(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
    
        // Gọi thẳng sang Service, Service sẽ lo từ A-Z (gọi Leonardo + lưu DB)
        // Chú ý: Chữ 'null' ở đây là đại diện cho User. Nếu đã có chức năng lấy User đang đăng nhập, hãy thay null bằng object User đó.
        com.socialmanager.model.ImageGeneration imgGen = imageGenService.startImageGeneration(prompt, null);
    
        if (imgGen != null) {
            return ResponseEntity.ok(Map.of(
                "message", "Đã gửi yêu cầu, vui lòng đợi AI vẽ ảnh!", 
                "generationId", imgGen.getLeonardoGenerationId()
            ));
        } else {
            return ResponseEntity.status(500).body(Map.of("error", "Có lỗi xảy ra khi gọi Leonardo"));
        }
    }
}