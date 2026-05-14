import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router";

export function AuthCallback() {
	const [searchParams] = useSearchParams();
	const navigate = useNavigate();

	useEffect(() => {
		const token = searchParams.get("token");
		
		if (token) {
			// Lưu token vào localStorage
			localStorage.setItem("token", token);
			
			// Chuyển hướng đến dashboard
			navigate("/", { replace: true });
		} else {
			// Nếu không có token, chuyển về trang login
			navigate("/login", { replace: true });
		}
	}, [searchParams, navigate]);

	return (
		<div className="flex flex-col justify-center items-center flex-1">
			<div className="text-lg">Đang xử lý đăng nhập...</div>
		</div>
	);
}
