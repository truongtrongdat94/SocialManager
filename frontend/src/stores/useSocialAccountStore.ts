import { create } from "zustand";

import type { SocialAccount, Platform } from "@/types";
import { AccountsApi } from "@/apis";

interface SocialAccountsState {
	accounts: SocialAccount[];
	fetchAccounts: () => Promise<void>;
	connectAccount: (platform: Platform) => Promise<void>;
	deleteAccount: (id: string) => Promise<void>;
}

export const useSocialAccountStore =
	create<SocialAccountsState>((set, get) => ({
		accounts: [],

		fetchAccounts: async () => {
			try {
				const res = await AccountsApi.getSocialAccounts();
				set({ accounts: res.data.data });
			} catch (err) {
				console.error(err);
			}
		},

		connectAccount: async (platform) => {
			try {
				const res = await AccountsApi.connect(platform);
				window.location.href = res.data.data;
			} catch (err) {
				console.error(err);
			}
		},

		deleteAccount: async (id) => {
			try {
				await AccountsApi.deleteSocialAccount(id);

				set({
					accounts: get().accounts.filter(
						(acc) => acc.id !== id
					),
				});
			} catch (err) {
				console.error(err);
			}
		},
	}));