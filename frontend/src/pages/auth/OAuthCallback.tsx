import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router";
import toast from "react-hot-toast";

export function OAuthCallback() {
	const navigate = useNavigate();
	const [searchParams] = useSearchParams();

	useEffect(() => {
		const accessToken = searchParams.get("accessToken");
		const refreshToken = searchParams.get("refreshToken");

		if (accessToken && refreshToken) {
			localStorage.setItem("accessToken", accessToken);
			localStorage.setItem("refreshToken", refreshToken);
			toast.success("Đăng nhập thành công");
			navigate("/dashboard/ai");
		} else {
			toast.error("Đăng nhập thất bại");
			navigate("/login");
		}
	}, [searchParams, navigate]);

	return (
		<div className="flex min-h-screen items-center justify-center bg-[#d7f4ff]">
			<div className="text-center">
				<div className="mb-4 inline-block h-12 w-12 animate-spin rounded-full border-4 border-sky-200 border-t-sky-500"></div>
				<p className="text-lg font-semibold text-sky-700">Đang xử lý đăng nhập...</p>
			</div>
		</div>
	);
}
