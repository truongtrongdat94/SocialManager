import { useState } from "react";
import { Button, Input } from "@/components";
import {icons} from "@/constants/icons.ts";
import { Eye, EyeClosed } from "lucide-react";
import {useNavigate} from "react-router";

export function Register() {
	const [showPassword, setShowPassword] = useState(false);

	const navigate = useNavigate();

	return (
		<div className="flex flex-col justify-center items-center flex-1 w-100 mx-auto">
			<div className="text-xl font-bold mb-8">Đăng ký tài khoản</div>
			<div className="w-full flex flex-col items-center gap-6">
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Tên hiển thị</div>
					<Input placeholder="Nguyễn Văn A" className="flex-1" />
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Username</div>
					<Input className="flex-1" />
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Email</div>
					<Input placeholder="example@email.com" className="flex-1" />
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Mật khẩu</div>
					<Input iconButtonFunction={() =>
						setShowPassword((prev) => !prev)
					} iconPosition="right" type="password" className="flex-1">
						{!showPassword ? (
							<Eye size={20} strokeWidth={1.5}/>
						) : (
							<EyeClosed size={20} strokeWidth={1.5}/>
						)}
					</Input>
				</div>
				<Button variant="solid" color="primary" size="lg" className="w-full">
					Tạo tài khoản
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
			</div>
		</div>
	);
}