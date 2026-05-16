import { create } from "zustand";
import type { SocialAccount } from "@/types";
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
}));