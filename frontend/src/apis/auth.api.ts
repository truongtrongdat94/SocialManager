import axios from "@/libs/axios.ts"

export interface LoginRequest {
	username: string;
	password: string;
}

export interface RegisterRequest {
	username: string;
	email: string;
	password: string;
}

export interface AuthResponse {
	token: string;
}

export interface UserDto {
	id: string;
	username: string;
	email: string;
}

class AuthApi {
	static async register(data: RegisterRequest) {
		const response = await axios.post<{ data: UserDto }>("/auth/register", data);
		return response.data.data;
	}

	static async login(data: LoginRequest) {
		const response = await axios.post<{ data: AuthResponse }>("/auth/login", data);
		return response.data.data;
	}

	static async getCurrentUser() {
		const response = await axios.get<{ data: UserDto }>("/auth/me");
		return response.data.data;
	}
}

export default AuthApi;