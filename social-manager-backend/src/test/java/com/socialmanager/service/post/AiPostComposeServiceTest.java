package com.socialmanager.service.post;

import com.socialmanager.dto.AiPostComposeRequest;
import com.socialmanager.dto.AiPostComposeResponse;
import com.socialmanager.dto.ScheduledPostResponse;
import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.AiGenerationLog;
import com.socialmanager.model.ImageGeneration;
import com.socialmanager.model.User;
import com.socialmanager.repository.AiGenerationLogRepository;
import com.socialmanager.repository.ImageGenerationRepository;
import com.socialmanager.service.account.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPostComposeServiceTest {

    @Mock
    private PostService postService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AiGenerationLogRepository aiGenerationLogRepository;

    @Mock
    private ImageGenerationRepository imageGenerationRepository;

    @InjectMocks
    private AiPostComposeService aiPostComposeService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void previewUsesAiLogWhenOverrideMissing() {
        UUID aiLogId = UUID.randomUUID();

        AiGenerationLog aiLog = AiGenerationLog.builder()
                .id(aiLogId)
                .resultCaption("AI generated caption")
                .imageUrls(new String[]{"https://cdn.example.com/generated-1.png"})
                .build();

        when(aiGenerationLogRepository.findByIdAndUser_Id(aiLogId, userId)).thenReturn(Optional.of(aiLog));
        when(postService.preview(any())).thenReturn(mockPostResponse("PREVIEW"));

        AiPostComposeRequest request = new AiPostComposeRequest();
        request.setSocialAccountId(UUID.randomUUID().toString());
        request.setScheduledTime(LocalDateTime.now().plusMinutes(10));
        request.setAiGenerationLogId(aiLogId.toString());

        AiPostComposeResponse response = aiPostComposeService.preview(request);

        assertEquals("AI_GENERATION_LOG", response.getContentSource());
        assertEquals(List.of("https://cdn.example.com/generated-1.png"), response.getResolvedMediaUrls());
        assertEquals(aiLogId.toString(), response.getAiGenerationLogId());

        ArgumentCaptor<com.socialmanager.dto.ScheduledPostRequest> captor = ArgumentCaptor.forClass(com.socialmanager.dto.ScheduledPostRequest.class);
        verify(postService).preview(captor.capture());
        assertEquals("AI generated caption", captor.getValue().getContent());
    }

    @Test
    void scheduleUsesOverrideAndSkipsSourceLookups() {
        when(postService.createScheduledPost(any())).thenReturn(mockPostResponse("PENDING"));

        AiPostComposeRequest request = new AiPostComposeRequest();
        request.setSocialAccountId(UUID.randomUUID().toString());
        request.setScheduledTime(LocalDateTime.now().plusHours(2));
        request.setContentOverride("Manual override content");
        request.setMediaUrls(List.of("https://cdn.example.com/manual.png"));

        AiPostComposeResponse response = aiPostComposeService.schedule(request);

        assertEquals("REQUEST_OVERRIDE", response.getContentSource());
        assertEquals(List.of("https://cdn.example.com/manual.png"), response.getResolvedMediaUrls());
        verify(aiGenerationLogRepository, never()).findByIdAndUser_Id(any(), any());
        verify(imageGenerationRepository, never()).findByIdAndUser_Id(any(), any());
    }

    @Test
    void throwsWhenNoContentFromAnySource() {
        UUID aiLogId = UUID.randomUUID();
        UUID imageGenerationId = UUID.randomUUID();

        AiGenerationLog aiLog = AiGenerationLog.builder()
                .id(aiLogId)
                .resultCaption("  ")
                .build();

        ImageGeneration imageGeneration = ImageGeneration.builder()
                .id(imageGenerationId)
                .caption(" ")
                .build();

        when(aiGenerationLogRepository.findByIdAndUser_Id(aiLogId, userId)).thenReturn(Optional.of(aiLog));
        when(imageGenerationRepository.findByIdAndUser_Id(imageGenerationId, userId)).thenReturn(Optional.of(imageGeneration));

        AiPostComposeRequest request = new AiPostComposeRequest();
        request.setSocialAccountId(UUID.randomUUID().toString());
        request.setScheduledTime(LocalDateTime.now().plusMinutes(30));
        request.setAiGenerationLogId(aiLogId.toString());
        request.setImageGenerationId(imageGenerationId.toString());

        assertThrows(BusinessException.class, () -> aiPostComposeService.preview(request));
    }

    private ScheduledPostResponse mockPostResponse(String status) {
        ScheduledPostResponse response = new ScheduledPostResponse();
        response.setId(UUID.randomUUID().toString());
        response.setStatus(status);
        response.setScheduledTime(LocalDateTime.now().plusMinutes(5));
        return response;
    }
}
