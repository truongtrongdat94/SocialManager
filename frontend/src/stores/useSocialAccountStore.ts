import { create } from "zustand";
import type { SocialAccount } from "@/types";

interface SocialAccountsState {
	accounts: SocialAccount[];
}

export const useSocialAccountStore = create<SocialAccountsState>(() => ({
	accounts: [
		{
			id: "1",
			accountName: "Social Manager",
			platform: "FACEBOOK",
			profilePictureUrl:
				"https://i.pravatar.cc/150?img=1",
		},
		{
			id: "2",
			accountName: "my_instagram",
			platform: "INSTAGRAM",
			profilePictureUrl:
				"https://i.pravatar.cc/150?img=2",
		},
		{
			id: "3",
			accountName: "threads_account",
			platform: "THREADS",
			profilePictureUrl:
				"https://i.pravatar.cc/150?img=3",
		},
		{
			id: "4",
			accountName: "tiktok.channel",
			platform: "TIKTOK",
			profilePictureUrl:
				"https://i.pravatar.cc/150?img=4",
		},

	],
}));