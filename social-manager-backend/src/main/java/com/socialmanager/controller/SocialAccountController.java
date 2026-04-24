package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.SocialAccountCreateRequest;
import com.socialmanager.dto.SocialAccountResponse;
import com.socialmanager.dto.SocialAccountUpdateRequest;
import com.socialmanager.service.SocialAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/social-accounts")
@RequiredArgsConstructor
public class SocialAccountController {

    private final SocialAccountService socialAccountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SocialAccountResponse>>> listAccounts() {
        return ResponseEntity.ok(ApiResponse.ok(socialAccountService.listAccounts()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SocialAccountResponse>> createAccount(
            @Valid @RequestBody SocialAccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(socialAccountService.createAccount(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SocialAccountResponse>> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(socialAccountService.getAccount(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SocialAccountResponse>> updateAccount(
            @PathVariable UUID id,
            @Valid @RequestBody SocialAccountUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(socialAccountService.updateAccount(id, request)));
    }

    @PatchMapping("/{id}/autopilot")
    public ResponseEntity<ApiResponse<SocialAccountResponse>> toggleAutoPilot(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(socialAccountService.toggleAutoPilot(id, enabled)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable UUID id) {
        socialAccountService.deleteAccount(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.ok(null));
    }
}