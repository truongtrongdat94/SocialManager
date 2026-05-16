import { useState } from "react";
import { Button, Input } from "@/components";
import {icons} from "@/constants/icons.ts";
import { Eye, EyeClosed } from "lucide-react";
import {useNavigate} from "react-router";
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
		}
	};

	return (
		<div className="flex flex-col justify-center items-center flex-1 w-100 mx-auto">
			<div className="text-xl font-bold mb-8">Đăng ký tài khoản</div>
			<form onSubmit={handleRegister} className="w-full flex flex-col items-center gap-6">
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
						required
					/>
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Email</div>
					<Input 
						type="email"
						value={form.email}
						onChange={(e) => setForm({ ...form, email: e.target.value })}
						placeholder="example@email.com" 
						disabled={isLoading}
						className="flex-1"
						required
					/>
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Mật khẩu</div>
					<Input 
						type={showPassword ? "text" : "password"}
						value={form.password}
						onChange={(e) => setForm({ ...form, password: e.target.value })}
						disabled={isLoading}
						iconButtonFunction={() => setShowPassword((prev) => !prev)}
						iconPosition="right" 
						className="flex-1"
						required
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
					{isLoading ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
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
				<a className="cursor-pointer"><img src={icons.google} alt="google" className="w-6 h-6"/></a>
				<div>
					<span>Đã có tài khoản?&nbsp;</span>
					<a onClick={() => navigate("/login")} className="text-accent font-medium cursor-pointer">Đăng nhập</a>
				</div>
			</form>
		</div>
	);
}