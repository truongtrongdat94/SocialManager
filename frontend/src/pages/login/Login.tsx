import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Button, Input } from "@/components";
import {icons} from "@/constants/icons.ts";
import { Eye, EyeClosed } from "lucide-react";
import AuthApi from "@/apis/auth.api";
import { getValidToken } from "@/utils/auth";

export function Login() {
	const [form, setForm] = useState({ username: "", password: "" });
	const [isLoading, setIsLoading] = useState(false);
	const [error, setError] = useState("");
	const [showPassword, setShowPassword] = useState(false);
	const navigate = useNavigate();

	const handleLogin = async (e: React.FormEvent) => {
		e.preventDefault();
		setError("");
		setIsLoading(true);

		try {
			const response = await AuthApi.login(form);
			const token = response.token;

			localStorage.setItem("token", token);
			navigate("/dashboard/accounts");
		} catch (err: any) {
			const message = err?.response?.data?.message || "Login failed";
			setError(message);
		} finally {
			setIsLoading(false);
		}
	};

	useEffect(() => {
		const token = getValidToken();
		if (token) {
			navigate("/dashboard/accounts");
		}
	}, [navigate]);

	const handleGoogleLogin = () => {
		const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";
		const root = baseUrl.replace(/\/api\/?$/, "");
		window.location.href = `${root}/oauth2/authorization/google`;
	};

	return (
		<div className="flex flex-col justify-center items-center flex-1 w-100 mx-auto">
			<div className="text-xl font-bold mb-8">Đăng nhập</div>
			<form onSubmit={handleLogin} className="w-full flex flex-col items-center gap-6">
				{error && (
					<div className="w-full p-3 bg-red-100 border border-red-400 text-red-700 rounded">
						{error}
					</div>
				)}
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Username</div>
					<Input
						value={form.username}
						onChange={(e) => setForm({ ...form, username: e.target.value })}
						placeholder="Nhập tên đăng nhập"
						disabled={isLoading}
						className="flex-1"
					/>
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Mật khẩu</div>
					<Input
						type={showPassword ? "text" : "password"}
						value={form.password}
						onChange={(e) => setForm({ ...form, password: e.target.value })}
						placeholder="Nhập mật khẩu"
						disabled={isLoading}
						iconButtonFunction={() => setShowPassword((prev) => !prev)}
						iconPosition="right"
						className="flex-1"
					>
						{!showPassword ? (
							<Eye size={20} strokeWidth={1.5}/>
						) : (
							<EyeClosed size={20} strokeWidth={1.5}/>
						)}
					</Input>
				</div>
				<Button 
					type="submit"
					variant="solid" 
					color="primary" 
					size="lg" 
					className="w-full"
					disabled={isLoading}
				>
					{isLoading ? "Đang đăng nhập..." : "Đăng nhập"}
				</Button>
				<a className="cursor-pointer text-accent font-medium">Quên mật khẩu?</a>
				<div className="relative flex items-center justify-center w-full">
					<div className="h-px flex-1 bg-divider"/>
					<span className="px-3 text-sm">
				        Hoặc
				    </span>
					<div className="h-px flex-1 bg-divider"/>
				</div>
				<button type="button" onClick={handleGoogleLogin} className="cursor-pointer">
					<img src={icons.google} alt="google" className="w-6 h-6"/>
				</button>
				<div>
					<span>Chưa có tài khoản?&nbsp;</span>
					<a onClick={() => navigate("/register")} className="text-accent font-medium cursor-pointer">Đăng ký ngay</a>
				</div>
			</form>
		</div>
	);
}

/*
        <div style={{ padding: "2rem", display: "flex", flexDirection: "column", gap: "0.5rem" }}>
            <h1>Login</h1>

            <div>

            </div>

            <input
                style={{width: "fit-content"}}
                placeholder="username"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
            />

            <input
                style={{width: "fit-content"}}
                placeholder="password"
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
            />

            <button style={{width: "fit-content"}} onClick={login}>Login</button>

            {isLoggedIn && (
                <div style={{marginTop: "1rem", display: "flex", flexDirection: "column", gap: "0.5rem"}}>
                    <h2>CONNECT SOCIAL</h2>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin(Platform.FACEBOOK)}>
                        Connect Facebook
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin(Platform.INSTAGRAM)}>
                        Connect Instagram
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin(Platform.THREADS)}>
                        Connect Threads
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin(Platform.TIKTOK)}>
                        Connect TikTok
                    </button>
                </div>
            )}
        </div>

 */