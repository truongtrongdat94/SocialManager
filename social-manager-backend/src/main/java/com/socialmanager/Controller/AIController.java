package com.socialmanager.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // Import thêm cái này
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.request.CaptionRequest;
import com.socialmanager.model.ImageGeneration; // Import Security
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
    private final UserRepository userRepository; // Inject thêm UserRepository để tìm DB

    /**
     * Hàm dùng chung để bắt thông tin User đang gọi API từ JWT Token
     */
    private User getCurrentAuthenticatedUser() {
        // Cái getName() này hiện tại do module Auth quyết định, khả năng cao nó đang trả về Username
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // SỬA Ở ĐÂY: Đổi findByEmail thành findByUsername
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Lỗi Auth: Không tìm thấy User có username là [" + currentUsername + "] trong Database!"));
    }

    @PostMapping("/generate-caption")
    public ResponseEntity<ApiResponse<ImageGeneration>> generate(@Valid @RequestBody CaptionRequest request) {
        // 1. Lấy User xịn từ Token
        User currentUser = getCurrentAuthenticatedUser(); 
        
        // 2. Truyền User xịn vào Service thay vì mockUser
        ImageGeneration result = geminiAIService.createCaption(
            request.getTopic(), 
            request.getPlatform(), 
            currentUser
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Đã sinh nội dung AI thành công", result));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        // (Tùy chọn: Sau này bạn có thể sửa hàm getAllHistory trong Service 
        // thành getHistoryByUser(getCurrentAuthenticatedUser()) để user nào chỉ thấy lịch sử của người đó)
        return ResponseEntity.ok(geminiAIService.getAllHistory());
    }

    @PostMapping("/generate-image")
    public ResponseEntity<?> generateImage(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        
        // 1. Lấy User xịn từ Token
        User currentUser = getCurrentAuthenticatedUser(); 
    
        // 2. Thay chữ 'null' bằng currentUser
        ImageGeneration imgGen = imageGenService.startImageGeneration(prompt, currentUser);
    
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