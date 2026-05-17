import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router";
import toast from "react-hot-toast";
import {
	api,
	type ApiResponse,
	type AuthResponse,
	type RegisterRequest,
	getApiErrorMessage,
} from "@/lib/api";

export function Register() {
	const navigate = useNavigate();
	const [form, setForm] = useState<RegisterRequest>({
		name: "",
		username: "",
		email: "",
		password: "",
	});
	const [confirmPassword, setConfirmPassword] = useState("");
	const [loading, setLoading] = useState(false);
	const [activeField, setActiveField] = useState<"name" | "username" | "email" | "password" | "confirm" | null>(
		null,
	);

	const eyeOffsetX = useMemo(() => {
		if (activeField === "username" || activeField === "email") {
			return Math.max(-2, Math.min(3, form.username.length * 0.3));
		}
		return 0;
	}, [activeField, form.username.length]);

	const eyeOffsetY = useMemo(() => {
		if (activeField === "password" || activeField === "confirm") return 1.3;
		if (activeField === "name") return -0.5;
		return 0;
	}, [activeField]);

	const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
		event.preventDefault();

		if (!form.name.trim() || !form.username.trim() || !form.email.trim() || !form.password.trim() || !confirmPassword.trim()) {
			toast.error("Vui lòng nhập đầy đủ thông tin.");
			return;
		}

		if (form.password.length < 8) {
			toast.error("Mật khẩu phải có ít nhất 8 ký tự.");
			return;
		}

		if (form.password !== confirmPassword) {
			toast.error("Mật khẩu xác nhận chưa khớp.");
			return;
		}

		try {
			setLoading(true);
			await api.post<ApiResponse<unknown>>("/api/auth/register", form);

			const loginResponse = await api.post<ApiResponse<AuthResponse>>("/api/auth/login", {
				username: form.username,
				password: form.password,
			});

			const { accessToken, refreshToken } = loginResponse.data.data;
			localStorage.setItem("accessToken", accessToken);
			localStorage.setItem("refreshToken", refreshToken);
			toast.success("Đăng ký thành công");
			navigate("/dashboard/ai");
		} catch (error) {
			toast.error(getApiErrorMessage(error));
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="min-h-screen bg-[#d7f4ff] px-4 py-8 md:py-10">
			<div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-6xl items-center justify-center">
				<div className="w-full max-w-[620px] rounded-xl border-2 border-sky-100 bg-white p-7 shadow-[0_14px_34px_rgba(22,130,178,0.22)] md:p-8">
					<h1 className="mb-4 text-center text-[23px] font-extrabold tracking-wide text-sky-500">REGISTER</h1>

					<div className="mb-5 flex justify-center">
						<div className="relative h-[230px] w-[230px] rounded-full border-[4px] border-sky-300 bg-[#ebf9ff]">
							<svg
								viewBox="0 0 220 220"
								className="absolute inset-0 h-full w-full"
								fill="none"
								xmlns="http://www.w3.org/2000/svg"
							>
								<style>{`
                  .ring-firework {
                    transform-origin: 110px 110px;
                    animation: ringSpin 9s linear infinite;
                    opacity: 0.92;
                  }
                  .rainbow-burst {
                    transform-origin: 110px 110px;
                    animation: shootOut 2.2s ease-out infinite;
                  }
                  .rainbow-burst.delay-1 { animation-delay: 0.4s; }
                  .rainbow-burst.delay-2 { animation-delay: 0.85s; }
                  .rainbow-burst.delay-3 { animation-delay: 1.25s; }
                  @keyframes ringSpin {
                    from { transform: rotate(0deg); }
                    to { transform: rotate(360deg); }
                  }
                  @keyframes shootOut {
                    0% { opacity: 0; transform: scale(0.65); }
                    30% { opacity: 1; transform: scale(1); }
                    100% { opacity: 0; transform: scale(1.25); }
                  }
                `}</style>

								<circle cx="110" cy="110" r="109" stroke="#7BC9EB" strokeWidth="2" />

								<g
									className={`ring-firework ${loading ? "opacity-40" : ""}`}
									clipPath="inset(-50 -50 -50 -50)"
								>
									<g className="rainbow-burst">
										<path d="M110 1 L110 -15" stroke="#f43f5e" strokeWidth="3" strokeLinecap="round" />
										<path d="M203 46 L215 37" stroke="#f97316" strokeWidth="3" strokeLinecap="round" />
										<path d="M219 110 L236 110" stroke="#eab308" strokeWidth="3" strokeLinecap="round" />
										<path d="M203 173 L216 182" stroke="#22c55e" strokeWidth="3" strokeLinecap="round" />
										<path d="M110 219 L110 236" stroke="#06b6d4" strokeWidth="3" strokeLinecap="round" />
										<path d="M17 173 L4 182" stroke="#3b82f6" strokeWidth="3" strokeLinecap="round" />
										<path d="M1 110 L-16 110" stroke="#8b5cf6" strokeWidth="3" strokeLinecap="round" />
										<path d="M17 46 L4 37" stroke="#ec4899" strokeWidth="3" strokeLinecap="round" />
									</g>
									<g className="rainbow-burst delay-1">
										<path d="M143 7 L147 -10" stroke="#fb7185" strokeWidth="2.7" strokeLinecap="round" />
										<path d="M198 77 L214 70" stroke="#fb923c" strokeWidth="2.7" strokeLinecap="round" />
										<path d="M198 143 L214 150" stroke="#84cc16" strokeWidth="2.7" strokeLinecap="round" />
										<path d="M143 213 L147 230" stroke="#22d3ee" strokeWidth="2.7" strokeLinecap="round" />
										<path d="M77 213 L73 230" stroke="#60a5fa" strokeWidth="2.7" strokeLinecap="round" />
										<path d="M22 143 L6 150" stroke="#a78bfa" strokeWidth="2.7" strokeLinecap="round" />
										<path d="M22 77 L6 70" stroke="#f472b6" strokeWidth="2.7" strokeLinecap="round" />
										<path d="M77 7 L73 -10" stroke="#f43f5e" strokeWidth="2.7" strokeLinecap="round" />
									</g>
									<g className="rainbow-burst delay-2">
										<circle cx="110" cy="-18" r="3" fill="#f43f5e" />
										<circle cx="229" cy="48" r="3" fill="#f97316" />
										<circle cx="238" cy="110" r="3" fill="#eab308" />
										<circle cx="229" cy="172" r="3" fill="#22c55e" />
										<circle cx="110" cy="238" r="3" fill="#06b6d4" />
										<circle cx="-18" cy="172" r="3" fill="#3b82f6" />
										<circle cx="-18" cy="110" r="3" fill="#8b5cf6" />
										<circle cx="-18" cy="48" r="3" fill="#ec4899" />
									</g>
									<g className="rainbow-burst delay-3">
										<path d="M171 20 L181 8" stroke="#fb7185" strokeWidth="2.6" strokeLinecap="round" />
										<path d="M213 110 L228 110" stroke="#fb923c" strokeWidth="2.6" strokeLinecap="round" />
										<path d="M171 200 L181 212" stroke="#84cc16" strokeWidth="2.6" strokeLinecap="round" />
										<path d="M49 200 L39 212" stroke="#22d3ee" strokeWidth="2.6" strokeLinecap="round" />
										<path d="M7 110 L-8 110" stroke="#60a5fa" strokeWidth="2.6" strokeLinecap="round" />
										<path d="M49 20 L39 8" stroke="#a78bfa" strokeWidth="2.6" strokeLinecap="round" />
									</g>
								</g>

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
								<path d="M103 126 Q110 133 117 126" stroke="#111827" strokeWidth="2.6" strokeLinecap="round" />

								<path
									d="M58 118 Q32 84 46 52 Q56 26 82 32 Q72 62 73 89 Q73 109 62 122 Z"
									fill="white"
									stroke="#111827"
									strokeWidth="3"
								/>
								<path
									d="M162 118 Q188 84 174 52 Q164 26 138 32 Q148 62 147 89 Q147 109 158 122 Z"
									fill="white"
									stroke="#111827"
									strokeWidth="3"
								/>
							</svg>
						</div>
					</div>

					<form className="space-y-4" onSubmit={handleSubmit}>
						<input
							value={form.name}
							onFocus={() => setActiveField("name")}
							onBlur={() => setActiveField(null)}
							onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
							className="h-12 w-full rounded-[4px] border-2 border-sky-300 px-4 text-base outline-none transition focus:border-sky-500"
							placeholder="Tên hiển thị"
						/>

						<input
							value={form.username}
							onFocus={() => setActiveField("username")}
							onBlur={() => setActiveField(null)}
							onChange={(e) => setForm((prev) => ({ ...prev, username: e.target.value }))}
							className="h-12 w-full rounded-[4px] border-2 border-sky-300 px-4 text-base outline-none transition focus:border-sky-500"
							placeholder="Username"
						/>

						<input
							type="email"
							value={form.email}
							onFocus={() => setActiveField("email")}
							onBlur={() => setActiveField(null)}
							onChange={(e) => setForm((prev) => ({ ...prev, email: e.target.value }))}
							className="h-12 w-full rounded-[4px] border-2 border-sky-300 px-4 text-base outline-none transition focus:border-sky-500"
							placeholder="Email"
						/>

						<input
							type="password"
							value={form.password}
							onFocus={() => setActiveField("password")}
							onBlur={() => setActiveField(null)}
							onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
							className="h-12 w-full rounded-[4px] border-2 border-sky-300 px-4 text-base outline-none transition focus:border-sky-500"
							placeholder="Mật khẩu (ít nhất 8 ký tự)"
						/>

						<input
							type="password"
							value={confirmPassword}
							onFocus={() => setActiveField("confirm")}
							onBlur={() => setActiveField(null)}
							onChange={(e) => setConfirmPassword(e.target.value)}
							className="h-12 w-full rounded-[4px] border-2 border-sky-300 px-4 text-base outline-none transition focus:border-sky-500"
							placeholder="Xác nhận mật khẩu"
						/>

						<button
							type="submit"
							disabled={loading}
							className="h-12 w-full rounded-[4px] bg-sky-500 text-base font-bold text-white transition hover:bg-sky-600 disabled:cursor-not-allowed disabled:opacity-60"
						>
							{loading ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
						</button>
					</form>

					<p className="mt-5 text-center text-base text-sky-700">
						Đã có tài khoản?{" "}
						<Link to="/login" className="font-semibold underline underline-offset-2">
							Đăng nhập
						</Link>
					</p>
				</div>
			</div>
		</div>
	);
}
