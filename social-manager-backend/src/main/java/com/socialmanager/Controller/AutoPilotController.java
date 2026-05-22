package com.socialmanager.controller;

import com.socialmanager.dto.request.CreateAutoPilotRequest;
import com.socialmanager.dto.request.UpdateAutoPilotRequest;
import com.socialmanager.dto.response.AutoPilotConfigResponse;
import com.socialmanager.exception.ResourceNotFoundException;
import com.socialmanager.model.AutoPilotConfig;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.model.User;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.service.AutoPilotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/autopilot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auto Pilot", description = "AI Auto Pilot configuration and management")
public class AutoPilotController {

    private final AutoPilotService autoPilotService;
    private final SocialAccountRepository socialAccountRepository;

    @GetMapping
    @Operation(summary = "Get all auto pilot configs for current user")
    public ResponseEntity<List<AutoPilotConfigResponse>> getAllConfigs(
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Getting all auto pilot configs for user: {}", currentUser.getUsername());
        
        List<AutoPilotConfigResponse> configs = autoPilotService.getConfigsByUser(currentUser)
            .stream()
            .map(AutoPilotConfigResponse::from)
            .toList();
        
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get auto pilot config by ID")
    public ResponseEntity<AutoPilotConfigResponse> getConfigById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Getting auto pilot config: {} for user: {}", id, currentUser.getUsername());
        
        AutoPilotConfig config = autoPilotService.getConfigById(id);
        
        // Security check: ensure config belongs to current user
        if (!config.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(AutoPilotConfigResponse.from(config));
    }

    @PostMapping
    @Operation(summary = "Create new auto pilot config")
    public ResponseEntity<AutoPilotConfigResponse> createConfig(
            @Valid @RequestBody CreateAutoPilotRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Creating auto pilot config for user: {}", currentUser.getUsername());
        
        // Verify social account belongs to user
        SocialAccount socialAccount = socialAccountRepository.findById(request.socialAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Social account not found: " + request.socialAccountId()));
        
        if (!socialAccount.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Build config
        AutoPilotConfig config = AutoPilotConfig.builder()
            .user(currentUser)
            .socialAccount(socialAccount)
            .keywords(request.keywords())
            .frequencyHours(request.frequencyHours())
            .status(request.status())
            .promptTemplate(request.promptTemplate())
            .nextRunAt(LocalDateTime.now().plusHours(request.frequencyHours()))
            .build();
        
        AutoPilotConfig saved = autoPilotService.createConfig(config);
        
        log.info("Created auto pilot config: {} for user: {}", saved.getId(), currentUser.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AutoPilotConfigResponse.from(saved));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update auto pilot config")
    public ResponseEntity<AutoPilotConfigResponse> updateConfig(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAutoPilotRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Updating auto pilot config: {} for user: {}", id, currentUser.getUsername());
        
        AutoPilotConfig existing = autoPilotService.getConfigById(id);
        
        // Security check
        if (!existing.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Build updates
        AutoPilotConfig updates = AutoPilotConfig.builder()
            .keywords(request.keywords())
            .frequencyHours(request.frequencyHours())
            .status(request.status())
            .promptTemplate(request.promptTemplate())
            .build();
        
        AutoPilotConfig updated = autoPilotService.updateConfig(id, updates);
        
        log.info("Updated auto pilot config: {}", id);
        
        return ResponseEntity.ok(AutoPilotConfigResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete auto pilot config")
    public ResponseEntity<Void> deleteConfig(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Deleting auto pilot config: {} for user: {}", id, currentUser.getUsername());
        
        AutoPilotConfig existing = autoPilotService.getConfigById(id);
        
        // Security check
        if (!existing.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        autoPilotService.deleteConfig(id);
        
        log.info("Deleted auto pilot config: {}", id);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "Manually trigger auto pilot execution")
    public ResponseEntity<String> triggerAutoPilot(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Manually triggering auto pilot config: {} for user: {}", id, currentUser.getUsername());
        
        AutoPilotConfig config = autoPilotService.getConfigById(id);
        
        // Security check
        if (!config.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Execute immediately
        autoPilotService.executeAutoPilot(config);
        
        // Update next run time
        autoPilotService.updateNextRun(config);
        
        log.info("Manually triggered auto pilot config: {}", id);
        
        return ResponseEntity.ok("Auto pilot triggered successfully");
    }
}
