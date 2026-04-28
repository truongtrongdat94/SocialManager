package com.socialmanager.service.post;

import com.socialmanager.dto.AiPostComposeRequest;
import com.socialmanager.dto.AiPostComposeResponse;
import com.socialmanager.dto.AiPostSourcesResponse;
import com.socialmanager.dto.AiSourceOption;
import com.socialmanager.dto.ScheduledPostRequest;
import com.socialmanager.dto.ScheduledPostResponse;
import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.AiGenerationLog;
import com.socialmanager.model.ImageGeneration;
import com.socialmanager.repository.AiGenerationLogRepository;
import com.socialmanager.repository.ImageGenerationRepository;
import com.socialmanager.service.account.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiPostComposeService {

    private static final String SOURCE_REQUEST_OVERRIDE = "REQUEST_OVERRIDE";
    private static final String SOURCE_AI_GENERATION_LOG = "AI_GENERATION_LOG";
    private static final String SOURCE_IMAGE_GENERATION = "IMAGE_GENERATION";

    private final PostService postService;
    private final CurrentUserService currentUserService;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final ImageGenerationRepository imageGenerationRepository;

    public AiPostComposeResponse preview(AiPostComposeRequest request) {
        ResolvedPayload resolvedPayload = resolvePayload(request);
        ScheduledPostResponse postPreview = postService.preview(toScheduledPostRequest(request, resolvedPayload));
        return toComposeResponse(postPreview, resolvedPayload);
    }

    public AiPostComposeResponse schedule(AiPostComposeRequest request) {
        ResolvedPayload resolvedPayload = resolvePayload(request);
        ScheduledPostResponse scheduledPost = postService.createScheduledPost(toScheduledPostRequest(request, resolvedPayload));
        return toComposeResponse(scheduledPost, resolvedPayload);
    }

        public AiPostSourcesResponse listSources(int limit) {
        UUID userId = currentUserService.getCurrentUser().getId();
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        List<AiSourceOption> aiLogs = aiGenerationLogRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageRequest)
            .stream()
            .map(item -> AiSourceOption.builder()
                .id(item.getId().toString())
                .contentPreview(truncate(trimToNull(item.getResultCaption()), 120))
                .mediaCount(item.getImageUrls() == null ? 0 : item.getImageUrls().length)
                .createdAt(item.getCreatedAt())
                .build())
            .toList();

        List<AiSourceOption> imageGenerations = imageGenerationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageRequest)
            .stream()
            .map(item -> AiSourceOption.builder()
                .id(item.getId().toString())
                .contentPreview(truncate(trimToNull(item.getCaption()), 120))
                .mediaCount(item.getCloudinaryUrls() == null ? 0 : item.getCloudinaryUrls().length)
                .createdAt(item.getCreatedAt())
                .build())
            .toList();

        return AiPostSourcesResponse.builder()
            .aiGenerationLogs(aiLogs)
            .imageGenerations(imageGenerations)
            .build();
        }

    private ResolvedPayload resolvePayload(AiPostComposeRequest request) {
        UUID userId = currentUserService.getCurrentUser().getId();

        UUID aiGenerationLogId = parseOptionalUuid(request.getAiGenerationLogId(), "aiGenerationLogId");
        UUID imageGenerationId = parseOptionalUuid(request.getImageGenerationId(), "imageGenerationId");

        AiGenerationLog aiLog = null;
        if (aiGenerationLogId != null) {
            aiLog = aiGenerationLogRepository.findByIdAndUser_Id(aiGenerationLogId, userId)
                    .orElseThrow(() -> new BusinessException("AiGenerationLog not found"));
        }

        ImageGeneration imageGeneration = null;
        if (imageGenerationId != null) {
            imageGeneration = imageGenerationRepository.findByIdAndUser_Id(imageGenerationId, userId)
                    .orElseThrow(() -> new BusinessException("ImageGeneration not found"));
        }

        String content = trimToNull(request.getContentOverride());
        String contentSource = SOURCE_REQUEST_OVERRIDE;

        if (content == null && aiLog != null) {
            content = trimToNull(aiLog.getResultCaption());
            if (content != null) {
                contentSource = SOURCE_AI_GENERATION_LOG;
            }
        }

        if (content == null && imageGeneration != null) {
            content = trimToNull(imageGeneration.getCaption());
            if (content != null) {
                contentSource = SOURCE_IMAGE_GENERATION;
            }
        }

        if (content == null) {
            throw new BusinessException("No content available from request override or AI generation sources");
        }

        List<String> resolvedMediaUrls = mergeMediaUrls(request.getMediaUrl(), request.getMediaUrls());
        if (resolvedMediaUrls.isEmpty() && aiLog != null) {
            resolvedMediaUrls = mergeMediaUrls(null, toList(aiLog.getImageUrls()));
        }
        if (resolvedMediaUrls.isEmpty() && imageGeneration != null) {
            resolvedMediaUrls = mergeMediaUrls(null, toList(imageGeneration.getCloudinaryUrls()));
        }

        return new ResolvedPayload(
                content,
                contentSource,
                resolvedMediaUrls,
                aiGenerationLogId,
                imageGenerationId
        );
    }

    private ScheduledPostRequest toScheduledPostRequest(AiPostComposeRequest request, ResolvedPayload resolvedPayload) {
        ScheduledPostRequest scheduledPostRequest = new ScheduledPostRequest();
        scheduledPostRequest.setSocialAccountId(request.getSocialAccountId());
        scheduledPostRequest.setScheduledTime(request.getScheduledTime());
        scheduledPostRequest.setContent(resolvedPayload.content());

        if (!resolvedPayload.mediaUrls().isEmpty()) {
            scheduledPostRequest.setMediaUrls(resolvedPayload.mediaUrls());
            scheduledPostRequest.setMediaUrl(resolvedPayload.mediaUrls().get(0));
        }

        return scheduledPostRequest;
    }

    private AiPostComposeResponse toComposeResponse(ScheduledPostResponse response, ResolvedPayload resolvedPayload) {
        return AiPostComposeResponse.builder()
                .post(response)
                .contentSource(resolvedPayload.contentSource())
                .resolvedMediaUrls(resolvedPayload.mediaUrls())
                .aiGenerationLogId(resolvedPayload.aiGenerationLogId() == null ? null : resolvedPayload.aiGenerationLogId().toString())
                .imageGenerationId(resolvedPayload.imageGenerationId() == null ? null : resolvedPayload.imageGenerationId().toString())
                .build();
    }

    private List<String> mergeMediaUrls(String mediaUrl, List<String> mediaUrls) {
        List<String> merged = new ArrayList<>();
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            merged.add(mediaUrl.trim());
        }

        if (mediaUrls != null) {
            mediaUrls.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(merged::add);
        }

        return new ArrayList<>(new LinkedHashSet<>(merged));
    }

    private List<String> toList(String[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private UUID parseOptionalUuid(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawValue.trim());
        } catch (Exception ex) {
            throw new BusinessException(fieldName + " must be a valid UUID");
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }

    private record ResolvedPayload(
            String content,
            String contentSource,
            List<String> mediaUrls,
            UUID aiGenerationLogId,
            UUID imageGenerationId
    ) {
    }
}
