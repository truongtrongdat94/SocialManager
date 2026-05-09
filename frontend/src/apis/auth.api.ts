import axios from "@/libs/axios.ts"

class AuthApi {
	static async register() {
		return await axios.post("/auth/register", {})
	}

	static async login() {

	}
}

export default AuthApi;