import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router";
import toast from "react-hot-toast";
import { api, type ApiResponse, type AuthResponse, getApiErrorMessage } from "@/lib/api";

interface LoginFormState {
	username: string;
	password: string;
}

export function Login() {
	const navigate = useNavigate();
	const [form, setForm] = useState<LoginFormState>({ username: "", password: "" });
	const [loading, setLoading] = useState(false);
	const [showPassword, setShowPassword] = useState(false);
	const [activeField, setActiveField] = useState<"username" | "password" | null>(null);

	const eyeOffsetX = useMemo(() => {
		if (showPassword) return 0.5;
		if (activeField !== "username") return 0;
		return Math.max(-2.5, Math.min(3.5, form.username.length * 0.35 - 0.5));
	}, [activeField, form.username.length, showPassword]);

	const eyeOffsetY = useMemo(() => {
		if (showPassword) return -0.2;
		if (activeField === "password") return 1.4;
		if (activeField === "username") return -0.4;
		return 0;
	}, [activeField, showPassword]);

	const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
		event.preventDefault();

		if (!form.username.trim() || !form.password.trim()) {
			toast.error("Vui lòng nhập đầy đủ username và mật khẩu.");
			return;
		}

		try {
			setLoading(true);
			const response = await api.post<ApiResponse<AuthResponse>>("/api/auth/login", form);
			const { accessToken, refreshToken } = response.data.data;
			localStorage.setItem("accessToken", accessToken);
			localStorage.setItem("refreshToken", refreshToken);
			toast.success("Đăng nhập thành công");
			navigate("/dashboard/ai");
		} catch (error) {
			toast.error(getApiErrorMessage(error));
		} finally {
			setLoading(false);
		}
	};

	const handleGoogleLogin = () => {
		const backendUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
		window.location.href = `${backendUrl}/oauth2/authorization/google`;
	};

	return (
		<div className="min-h-screen bg-[#d7f4ff] px-4 py-8 md:py-10">
			<div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-6xl items-center justify-center">
				<div className="w-full max-w-[560px] rounded-xl border-2 border-sky-100 bg-white p-7 shadow-[0_14px_34px_rgba(22,130,178,0.22)] md:p-8">
					<h1 className="mb-4 text-center text-[23px] font-extrabold tracking-wide text-sky-500">LOGIN</h1>

					<div className="mb-5 flex justify-center">
						<div className="relative h-[230px] w-[230px] rounded-full border-[4px] border-sky-300 bg-[#ebf9ff]">
							<svg
								viewBox="0 0 220 220"
								className="absolute inset-0 h-full w-full"
								fill="none"
								xmlns="http://www.w3.org/2000/svg"
							>
								<circle cx="110" cy="110" r="109" stroke="#7BC9EB" strokeWidth="2" />

								<circle cx="75" cy="52" r="19" fill="white" stroke="#111827" strokeWidth="3" />
								<circle cx="145" cy="52" r="19" fill="white" stroke="#111827" strokeWidth="3" />
								<circle cx="75" cy="52" r="8" fill="white" stroke="#111827" strokeWidth="2.5" />
								<circle cx="145" cy="52" r="8" fill="white" stroke="#111827" strokeWidth="2.5" />

								<path
									d="M58 185 C45 160, 47 98, 67 73 C81 56, 96 52, 110 52 C124 52, 139 56, 153 73 C173 98, 175 160, 162 185"
									fill="white"
									stroke="#111827"
									strokeWidth="3.3"
									strokeLinecap="round"
								/>

								<ellipse cx="110" cy="187" rx="31" ry="14" fill="#e2f4ff" />

								<g transform={`translate(${eyeOffsetX} ${eyeOffsetY})`}>
									<circle cx="92" cy="95" r="4.6" fill="#111827" />
									<circle cx="128" cy="95" r="4.6" fill="#111827" />
								</g>

								<ellipse cx="110" cy="111" rx="13" ry="9" fill="white" stroke="#111827" strokeWidth="2.8" />
								<ellipse cx="110" cy="112" rx="5.3" ry="4.2" fill="#111827" />
								<path d="M103 126 Q110 131 117 126" stroke="#111827" strokeWidth="2.6" strokeLinecap="round" />

								{showPassword && (
									<>
										<path
											d="M60 98 Q80 86 96 92 Q92 106 80 112 Q69 116 60 110 Z"
											fill="white"
											stroke="#111827"
											strokeWidth="3"
										/>
										<path
											d="M160 101 Q148 89 135 92 Q138 103 146 108 Q152 111 160 108 Z"
											fill="white"
											stroke="#111827"
											strokeWidth="3"
										/>
										<ellipse cx="116" cy="95.5" rx="2.8" ry="1" fill="#111827" />
									</>
								)}

								{!showPassword && (
									<>
										<path
											d="M52 115 Q70 96 84 101 Q74 118 65 126 Q57 127 52 121 Z"
											fill="white"
											stroke="#111827"
											strokeWidth="3"
										/>
										<path
											d="M168 115 Q150 96 136 101 Q146 118 155 126 Q163 127 168 121 Z"
											fill="white"
											stroke="#111827"
											strokeWidth="3"
										/>
									</>
								)}
							</svg>
						</div>
					</div>

					<form className="space-y-4" onSubmit={handleSubmit}>
						<input
							value={form.username}
							onFocus={() => setActiveField("username")}
							onBlur={() => setActiveField(null)}
							onChange={(e) => setForm((prev) => ({ ...prev, username: e.target.value }))}
							className="h-12 w-full rounded-[4px] border-2 border-sky-300 px-4 text-base outline-none transition focus:border-sky-500"
							placeholder="username"
							autoComplete="username"
						/>

						<div className="relative">
							<input
								type={showPassword ? "text" : "password"}
								value={form.password}
								onFocus={() => setActiveField("password")}
								onBlur={() => setActiveField(null)}
								onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
								className="h-12 w-full rounded-[4px] border-2 border-sky-300 px-4 pr-20 text-base outline-none transition focus:border-sky-500"
								placeholder="password"
								autoComplete="current-password"
							/>
							<button
								type="button"
								onClick={() => setShowPassword((prev) => !prev)}
								className="absolute right-2 top-1/2 -translate-y-1/2 rounded px-2 py-1 text-sm font-semibold text-sky-600 hover:bg-sky-50"
							>
								{showPassword ? "Ẩn" : "Hiện"}
							</button>
						</div>

						<button
							type="submit"
							disabled={loading}
							className="h-12 w-full rounded-[4px] bg-sky-500 text-base font-bold text-white transition hover:bg-sky-600 disabled:cursor-not-allowed disabled:opacity-60"
						>
							{loading ? "Đang đăng nhập..." : "Login"}
						</button>
					</form>

					<div className="my-4 flex items-center gap-3">
						<div className="h-px flex-1 bg-sky-200"></div>
						<span className="text-sm font-medium text-sky-600">hoặc</span>
						<div className="h-px flex-1 bg-sky-200"></div>
					</div>

					<button
						type="button"
						onClick={handleGoogleLogin}
						className="flex h-12 w-full items-center justify-center gap-3 rounded-[4px] border-2 border-sky-300 bg-white text-base font-semibold text-sky-700 transition hover:bg-sky-50"
					>
						<img src="/google-icon.svg" alt="Google" className="h-5 w-5" />
						Đăng nhập với Google
					</button>

					<p className="mt-5 text-center text-base text-sky-700">
						Chưa có tài khoản?{" "}
						<Link to="/register" className="font-semibold underline underline-offset-2">
							Đăng ký ngay
						</Link>
					</p>
				</div>
			</div>
		</div>
	);
}
