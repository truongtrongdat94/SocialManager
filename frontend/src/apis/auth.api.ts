import axios from "@/libs/axios.ts"

export interface LoginRequest {
	username: string;
	password: string;
}

export interface RegisterRequest {
	username: string;
	email: string;
	password: string;
	name: string;
}

export interface AuthResponse {
	token: string;
}

class AuthApi {
	static async register(data: RegisterRequest) {
		return await axios.post<{ data: { id: number; username: string; email: string; name: string } }>("/auth/register", data);
	}

	static async login(data: LoginRequest) {
		return await axios.post<{ data: AuthResponse }>("/auth/login", data);
	}

	static async getMe() {
		return await axios.get<{ data: { id: number; username: string; email: string; name: string } }>("/auth/me");
	}
}

export default AuthApi;