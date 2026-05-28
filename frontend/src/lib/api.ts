import axios, { type AxiosError } from "axios";

export interface ApiResponse<T> {
	success: boolean;
	data: T;
	message?: string;
}

export interface AuthResponse {
	accessToken: string;
	refreshToken?: string | null;
}

export interface UserDto {
	id: string;
	email: string;
	username: string;
	name: string;
}

export interface RegisterRequest {
	username: string;
	email: string;
	password: string;
	name: string;
}

export interface LoginRequest {
	username: string;
	password: string;
}

export interface SocialAccountDto {
	id: string;
	platform: "FACEBOOK" | "INSTAGRAM" | "THREADS" | "TIKTOK";
	accountName: string;
	accountId: string;
	accessToken: string;
	refreshToken: string;
	tokenExpiresAt: string | null;
	createdAt: string;
	updatedAt: string;
}

export interface AiGenerationLog {
	id: string;
	prompt: string;
	resultCaption: string;
	createdAt?: string;
}

export interface ImageGeneration {
	id: string;
	prompt: string;
	caption?: string;
	imageUrl?: string;
	cloudinaryUrl?: string;
	cloudinaryUrls?: string[];
	leonardoGenerationId?: string;
	status: string;
	createdAt?: string;
}

export type AnalyticsMap = Record<string, number[]>;

const rawBaseUrl =
	(import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "http://localhost:8080";
const baseURL = rawBaseUrl.replace(/\/+$/, "");

export const api = axios.create({
	baseURL,
	headers: {
		"Content-Type": "application/json",
	},
	withCredentials: true, // Enable sending cookies
});

api.interceptors.request.use((config) => {
	const token = localStorage.getItem("accessToken");
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});

let isRefreshing = false;
let failedQueue: Array<{
	resolve: (value?: unknown) => void;
	reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
	failedQueue.forEach((prom) => {
		if (error) {
			prom.reject(error);
		} else {
			prom.resolve(token);
		}
	});
	failedQueue = [];
};

api.interceptors.response.use(
	(response) => response,
	async (error: AxiosError<{ message?: string }>) => {
		const originalRequest = error.config as typeof error.config & { _retry?: boolean };

		if (error.response?.status === 401 && !originalRequest._retry) {
			if (isRefreshing) {
				return new Promise((resolve, reject) => {
					failedQueue.push({ resolve, reject });
				})
					.then((token) => {
						if (originalRequest.headers) {
							originalRequest.headers.Authorization = `Bearer ${token}`;
						}
						return api(originalRequest);
					})
					.catch((err) => Promise.reject(err));
			}

			originalRequest._retry = true;
			isRefreshing = true;

			try {
				// Call refresh endpoint - refresh token is sent automatically via cookie
				const response = await axios.post<ApiResponse<AuthResponse>>(
					`${baseURL}/api/auth/refresh`,
					{}, // Empty body - refresh token is in cookie
					{ withCredentials: true } // Enable sending cookies
				);
				const { accessToken } = response.data.data;

				localStorage.setItem("accessToken", accessToken);

				if (originalRequest.headers) {
					originalRequest.headers.Authorization = `Bearer ${accessToken}`;
				}

				processQueue(null, accessToken);
				return api(originalRequest);
			} catch (refreshError) {
				processQueue(refreshError, null);
				localStorage.removeItem("accessToken");
				if (!window.location.pathname.includes("/login")) {
					window.location.href = "/login";
				}
				return Promise.reject(refreshError);
			} finally {
				isRefreshing = false;
			}
		}

		return Promise.reject(error);
	}
);

export function getApiErrorMessage(error: unknown): string {
	if (axios.isAxiosError<{ message?: string }>(error)) {
		return (
			error.response?.data?.message ||
			error.message ||
			"Có lỗi mạng xảy ra, vui lòng thử lại."
		);
	}
	return "Đã có lỗi không xác định xảy ra.";
}
