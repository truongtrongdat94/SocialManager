import { create } from "zustand";
import type { SocialAccount, Platform } from "@/types";
import { AccountsApi } from "@/apis";

interface SocialAccountsState {
	accounts: SocialAccount[];
	loading: boolean;
	error: string | null;
	fetchAccounts: () => Promise<void>;
	connectAccount: (platform: Platform) => Promise<void>;
	deleteAccount: (id: string) => Promise<void>;
}

export const useSocialAccountStore = create<SocialAccountsState>((set, get) => ({
	accounts: [],
	loading: false,
	error: null,

	fetchAccounts: async () => {
		set({ loading: true, error: null });
		try {
			const res = await AccountsApi.getSocialAccounts();
			set({ accounts: res.data.data || [], loading: false });
		} catch (err: any) {
			set({ error: err?.response?.data?.message || "Failed to fetch accounts", loading: false });
		}
	},

	connectAccount: async (platform) => {
		try {
			const res = await AccountsApi.connect(platform);
			window.location.href = res.data.data;
		} catch (err) {
			console.error(err);
			throw err;
		}
	},

	deleteAccount: async (id) => {
		try {
			await AccountsApi.deleteSocialAccount(id);
			set({ accounts: get().accounts.filter((a) => a.id !== id) });
		} catch (err) {
			console.error(err);
			throw err;
		}
	},
}));
