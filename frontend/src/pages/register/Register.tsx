import { useState } from "react";
<<<<<<< HEAD
=======
import toast from "react-hot-toast";
>>>>>>> upstream/dev
import { Button, Input } from "@/components";
import {icons} from "@/constants/icons.ts";
import { Eye, EyeClosed } from "lucide-react";
import {useNavigate} from "react-router";
<<<<<<< HEAD
import AuthApi from "@/apis/auth.api";

export function Register() {
	const [form, setForm] = useState({ 
		username: "", 
		email: "", 
		password: "" 
	});
	const [isLoading, setIsLoading] = useState(false);
	const [error, setError] = useState("");
	const [showPassword, setShowPassword] = useState(false);
	const navigate = useNavigate();

	const handleRegister = async (e: React.FormEvent) => {
		e.preventDefault();
		setError("");
		setIsLoading(true);

		try {
			await AuthApi.register(form);
			alert("Đăng ký thành công! Vui lòng đăng nhập.");
			navigate("/login");
		} catch (err: any) {
			const message = err?.response?.data?.message || "Registration failed";
			setError(message);
		} finally {
			setIsLoading(false);
=======
import AuthApi from "@/apis/auth.api.ts";

export function Register() {
	const [showPassword, setShowPassword] = useState(false);
	const [name, setName] = useState("");
	const [username, setUsername] = useState("");
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState("");
	const [loading, setLoading] = useState(false);
	const navigate = useNavigate();

	const handleRegister = async () => {
		if (!name || !username || !email || !password) {
			toast.error("Vui lòng nhập đầy đủ thông tin");
			return;
		}

		if (password.length < 8) {
			toast.error("Mật khẩu phải có ít nhất 8 ký tự");
			return;
		}

		setLoading(true);
		setError("");

		try {
			await AuthApi.register({ username, email, password, name });
			
			toast.success("Đăng ký thành công! Đang đăng nhập...");
			
			// Đăng ký thành công, tự động đăng nhập
			const loginResponse = await AuthApi.login({ username, password });
			const token = loginResponse.data.data.token;
			
			// Lưu token vào localStorage
			localStorage.setItem("token", token);
			
			// Chuyển hướng đến dashboard
			navigate("/");
		} catch (err: any) {
			console.error("Register error:", err);
			const errorMessage = err.response?.data?.message || "Đã xảy ra lỗi. Vui lòng thử lại sau";
			
			setError(errorMessage);
			toast.error(errorMessage);
		} finally {
			setLoading(false);
		}
	};

	const handleGoogleRegister = () => {
		// Redirect to backend OAuth2 endpoint
		window.location.href = "http://localhost:8080/oauth2/authorization/google";
	};

	const handleKeyPress = (e: React.KeyboardEvent) => {
		if (e.key === "Enter") {
			handleRegister();
>>>>>>> upstream/dev
		}
	};

	return (
		<div className="flex flex-col justify-center items-center flex-1 w-100 mx-auto">
			<div className="text-xl font-bold mb-8">Đăng ký tài khoản</div>
<<<<<<< HEAD
			<form onSubmit={handleRegister} className="w-full flex flex-col items-center gap-6">
=======
			<div className="w-full flex flex-col items-center gap-6">
>>>>>>> upstream/dev
				{error && (
					<div className="w-full p-3 bg-red-100 border border-red-400 text-red-700 rounded">
						{error}
					</div>
				)}
				<div className="flex flex-col gap-2 w-full">
<<<<<<< HEAD
					<div className="font-medium">Username</div>
					<Input 
						value={form.username}
						onChange={(e) => setForm({ ...form, username: e.target.value })}
						placeholder="Nhập tên đăng nhập"
						disabled={isLoading}
						className="flex-1" 
						required
=======
					<div className="font-medium">Tên hiển thị</div>
					<Input 
						placeholder="Nguyễn Văn A" 
						className="flex-1"
						value={name}
						onChange={(e) => setName(e.target.value)}
						onKeyPress={handleKeyPress}
						disabled={loading}
					/>
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Username</div>
					<Input 
						className="flex-1"
						value={username}
						onChange={(e) => setUsername(e.target.value)}
						onKeyPress={handleKeyPress}
						disabled={loading}
>>>>>>> upstream/dev
					/>
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Email</div>
					<Input 
<<<<<<< HEAD
						type="email"
						value={form.email}
						onChange={(e) => setForm({ ...form, email: e.target.value })}
						placeholder="example@email.com" 
						disabled={isLoading}
						className="flex-1"
						required
=======
						placeholder="example@email.com" 
						className="flex-1"
						type="email"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
						onKeyPress={handleKeyPress}
						disabled={loading}
>>>>>>> upstream/dev
					/>
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Mật khẩu</div>
					<Input 
<<<<<<< HEAD
						type={showPassword ? "text" : "password"}
						value={form.password}
						onChange={(e) => setForm({ ...form, password: e.target.value })}
						disabled={isLoading}
						iconButtonFunction={() => setShowPassword((prev) => !prev)}
						iconPosition="right" 
						className="flex-1"
						required
=======
						iconButtonFunction={() => setShowPassword((prev) => !prev)} 
						iconPosition="right" 
						type={showPassword ? "text" : "password"} 
						className="flex-1"
						value={password}
						onChange={(e) => setPassword(e.target.value)}
						onKeyPress={handleKeyPress}
						disabled={loading}
>>>>>>> upstream/dev
					>
						{!showPassword ? (
							<Eye size={20} strokeWidth={1.5}/>
						) : (
							<EyeClosed size={20} strokeWidth={1.5}/>
						)}
					</Input>
				</div>
				<Button 
<<<<<<< HEAD
					type="submit"
=======
>>>>>>> upstream/dev
					variant="solid" 
					color="primary" 
					size="lg" 
					className="w-full"
<<<<<<< HEAD
					disabled={isLoading}
				>
					{isLoading ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
=======
					onClick={handleRegister}
					disabled={loading}
				>
					{loading ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
>>>>>>> upstream/dev
				</Button>
				<div className="text-sm text-center">
					<span>Bằng cách tạo tài khoản, bạn đồng ý với&nbsp;</span>
					<a className="cursor-pointer text-accent font-medium">điều khoản sử dụng</a>
					<span>&nbsp;và&nbsp;</span>
					<a className="cursor-pointer text-accent font-medium">chính sách bảo mật</a>
					<span>&nbsp;của chúng tôi</span>
				</div>
				<div className="relative flex items-center justify-center w-full">
					<div className="h-px flex-1 bg-divider"/>
					<span className="px-3 text-sm">
				        Hoặc
				    </span>
					<div className="h-px flex-1 bg-divider"/>
				</div>
<<<<<<< HEAD
				<a className="cursor-pointer"><img src={icons.google} alt="google" className="w-6 h-6"/></a>
=======
				<button 
					className="cursor-pointer"
					onClick={handleGoogleRegister}
					disabled={loading}
				>
					<img src={icons.google} alt="google" className="w-6 h-6"/>
				</button>
>>>>>>> upstream/dev
				<div>
					<span>Đã có tài khoản?&nbsp;</span>
					<a onClick={() => navigate("/login")} className="text-accent font-medium cursor-pointer">Đăng nhập</a>
				</div>
<<<<<<< HEAD
			</form>
=======
			</div>
>>>>>>> upstream/dev
		</div>
	);
}