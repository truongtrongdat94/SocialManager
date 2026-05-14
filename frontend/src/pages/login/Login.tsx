import { useState } from "react";
import { useNavigate } from "react-router";
import toast from "react-hot-toast";
import { Button, Input } from "@/components";
import {icons} from "@/constants/icons.ts";
import { Eye, EyeClosed } from "lucide-react";
import AuthApi from "@/apis/auth.api.ts";

export function Login() {
	const [showPassword, setShowPassword] = useState(false);
	const [username, setUsername] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState("");
	const [loading, setLoading] = useState(false);
	const navigate = useNavigate();

	const handleLogin = async () => {
		if (!username || !password) {
			toast.error("Vui lòng nhập đầy đủ thông tin");
			return;
		}

		setLoading(true);
		setError("");

		try {
			const response = await AuthApi.login({ username, password });
			const token = response.data.data.token;
			
			// Lưu token vào localStorage
			localStorage.setItem("token", token);
			
			toast.success("Đăng nhập thành công!");
			
			// Chuyển hướng đến dashboard
			navigate("/");
		} catch (err: any) {
			console.error("Login error:", err);
			const errorMessage = err.response?.data?.message || "Đã xảy ra lỗi. Vui lòng thử lại sau";
			
			setError(errorMessage);
			toast.error(errorMessage);
		} finally {
			setLoading(false);
		}
	};

	const handleGoogleLogin = () => {
		// Redirect to backend OAuth2 endpoint
		window.location.href = "http://localhost:8080/oauth2/authorization/google";
	};

	const handleKeyPress = (e: React.KeyboardEvent) => {
		if (e.key === "Enter") {
			handleLogin();
		}
	};

	return (
		<div className="flex flex-col justify-center items-center flex-1 w-100 mx-auto">
			<div className="text-xl font-bold mb-8">Đăng nhập</div>
			<div className="w-full flex flex-col items-center gap-6">
				{error && (
					<div className="w-full p-3 bg-red-100 border border-red-400 text-red-700 rounded">
						{error}
					</div>
				)}
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Username</div>
					<Input 
						className="flex-1"
						value={username}
						onChange={(e) => setUsername(e.target.value)}
						onKeyPress={handleKeyPress}
						disabled={loading}
					/>
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Mật khẩu</div>
					<Input 
						iconButtonFunction={() => setShowPassword((prev) => !prev)} 
						iconPosition="right" 
						type={showPassword ? "text" : "password"} 
						className="flex-1"
						value={password}
						onChange={(e) => setPassword(e.target.value)}
						onKeyPress={handleKeyPress}
						disabled={loading}
					>
						{!showPassword ? (
							<Eye size={20} strokeWidth={1.5}/>
						) : (
							<EyeClosed size={20} strokeWidth={1.5}/>
						)}
					</Input>
				</div>
				<Button 
					variant="solid" 
					color="primary" 
					size="lg" 
					className="w-full"
					onClick={handleLogin}
					disabled={loading}
				>
					{loading ? "Đang đăng nhập..." : "Đăng nhập"}
				</Button>
				<div className="relative flex items-center justify-center w-full">
					<div className="h-px flex-1 bg-divider"/>
					<span className="px-3 text-sm">
				        Hoặc
				    </span>
					<div className="h-px flex-1 bg-divider"/>
				</div>
				<button 
					className="cursor-pointer"
					onClick={handleGoogleLogin}
					disabled={loading}
				>
					<img src={icons.google} alt="google" className="w-6 h-6"/>
				</button>
				<div>
					<span>Chưa có tài khoản?&nbsp;</span>
					<a onClick={() => navigate("/register")} className="text-accent font-medium cursor-pointer">Đăng ký ngay</a>
				</div>
			</div>
		</div>
	);
}