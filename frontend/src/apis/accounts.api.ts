import axios from "@/libs/axios.ts"
import type { Platform } from "@/types";

class AccountsApi {
	static async connect(platform: Platform) {
		return await axios.get(`/social-accounts/connect/${platform}`);
	}

	static async getSocialAccountById(id: string) {
		return await axios.get(`/social-accounts/${id}`);
	}

	static async getSocialAccounts() {
		return await axios.get("/social-accounts");
	}

	static async deleteSocialAccount(id: string) {
		return await axios.delete(`/social-accounts/${id}`);
	}
}

export default AccountsApi;