import { create } from "zustand";
import type { SocialAccount } from "@/types";
<<<<<<< HEAD
import axiosInstance from "@/libs/axios";

interface SocialAccountsState {
	accounts: SocialAccount[];
	loading: boolean;
	error: string | null;
	fetchAccounts: () => Promise<void>;
}

export const useSocialAccountStore = create<SocialAccountsState>((set) => ({
	accounts: [],
	loading: false,
	error: null,
	fetchAccounts: async () => {
		set({ loading: true, error: null });
		try {
			const response = await axiosInstance.get<{ data: SocialAccount[] }>("/social-accounts");
			set({ accounts: response.data.data || [], loading: false });
		} catch (err: any) {
			set({ 
				error: err?.response?.data?.message || "Failed to fetch accounts",
				loading: false 
			});
		}
	},
=======

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
>>>>>>> upstream/dev
}));