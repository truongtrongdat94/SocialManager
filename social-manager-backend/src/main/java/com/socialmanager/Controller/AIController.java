package com.socialmanager.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.request.CaptionRequest;
import com.socialmanager.model.AiGenerationLog;
import com.socialmanager.model.ImageGeneration;
import com.socialmanager.model.User;
import com.socialmanager.repository.UserRepository;
import com.socialmanager.service.GeminiAIService;
import com.socialmanager.service.ImageGenService; 

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final GeminiAIService geminiAIService;
    private final ImageGenService imageGenService;
    private final UserRepository userRepository;

    /**
     * Hàm dùng chung để bắt thông tin User đang gọi API từ JWT Token
     */
    private User getCurrentAuthenticatedUser() {
        // getName() có thể trả về email (Google OAuth) hoặc username (local login)
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Tìm theo email trước, nếu không có thì tìm theo username
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new RuntimeException("Lỗi Auth: Không tìm thấy User có identifier [" + identifier + "] trong Database!"));
    }

    @PostMapping("/generate-caption")
    public ResponseEntity<ApiResponse<AiGenerationLog>> generate(@Valid @RequestBody CaptionRequest request) {
        User currentUser = getCurrentAuthenticatedUser(); 
    
        AiGenerationLog result = geminiAIService.createCaption(
            request.getTopic(), 
            request.getPlatform(), 
            currentUser
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Đã sinh nội dung AI thành công", result));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        User currentUser = getCurrentAuthenticatedUser();
        List<ImageGeneration> history = geminiAIService.getHistoryForUser(currentUser);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", history
        ));
    }

    @PostMapping("/generate-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateImage(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String caption = request.get("caption");
        
        try {
            User currentUser = getCurrentAuthenticatedUser();
            ImageGeneration imgGen = imageGenService.startImageGeneration(prompt, currentUser);

            if (caption != null && !caption.isBlank()) {
                imgGen.setCaption(caption);
            }
        
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Đã gửi yêu cầu, vui lòng đợi AI vẽ ảnh!",
                Map.of("generationId", imgGen.getLeonardoGenerationId())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>(
                false,
                "Không thể tạo ảnh AI vào lúc này",
                null
            ));
        }
    }
}