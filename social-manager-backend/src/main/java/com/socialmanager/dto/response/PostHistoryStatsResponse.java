package com.socialmanager.dto.response;

public record PostHistoryStatsResponse(
    long totalPosts,
    long manualPosts,
    long autoPilotPosts,
    long facebookPosts,
    long instagramPosts,
    long threadsPosts,
    long tiktokPosts
) {}
