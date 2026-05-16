import type { Platform } from "@/types/platform.type.ts";

// Chưa đầy đủ
export interface SocialAccount {
	id: string;
	accountName: string;
	platform: Platform;
	profilePictureUrl: string;
}