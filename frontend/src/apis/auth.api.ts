import axios from "@/libs/axios.ts"

export interface LoginRequest {
	username: string;
	password: string;
}

export interface RegisterRequest {
	username: string;
	email: string;
	password: string;
<<<<<<< HEAD
=======
	name: string;
>>>>>>> upstream/dev
}

export interface AuthResponse {
	token: string;
}

<<<<<<< HEAD
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
=======
class AuthApi {
	static async register(data: RegisterRequest) {
		return await axios.post<{ data: { id: number; username: string; email: string; name: string } }>("/auth/register", data);
	}

	static async login(data: LoginRequest) {
		return await axios.post<{ data: AuthResponse }>("/auth/login", data);
	}

	static async getMe() {
		return await axios.get<{ data: { id: number; username: string; email: string; name: string } }>("/auth/me");
>>>>>>> upstream/dev
	}
}

export default AuthApi;