import { useEffect, useState } from "react";
import { icons, platforms } from "@/constants"
import { Button } from "@/components"
import axiosInstance from "@/libs/axios";
import { getValidToken } from "@/utils/auth";

export const AddSocialAccountModal = () => {
	const [error, setError] = useState("");
	const [loadingPlatform, setLoadingPlatform] = useState("");
	const [available, setAvailable] = useState<Record<string, boolean>>({});
	const [configLoading, setConfigLoading] = useState(true);

	useEffect(() => {
		const loadConfig = async () => {
			try {
				const response = await axiosInstance.get<{ data: Record<string, boolean> }>("/social-accounts/config");
				setAvailable(response.data.data || {});
			} catch {
				setAvailable({});
			} finally {
				setConfigLoading(false);
			}
		};

		loadConfig();
	}, []);

	const handleConnect = async (platform: string) => {
		setError("");
		setLoadingPlatform(platform);
		const token = getValidToken();
		if (!token) {
			setError("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
			setLoadingPlatform("");
			window.location.href = "/login";
			return;
		}
		try {
			const response = await axiosInstance.get<{ data: string }>(`/social-accounts/connect/${platform.toUpperCase()}`);
			const url = response.data.data;
			if (!url) {
				setError("Không thể lấy đường dẫn kết nối.");
				return;
			}
			window.location.href = url;
		} catch (err: any) {
			setError(err?.response?.data?.message || "Không thể kết nối tài khoản lúc này.");
		} finally {
			setLoadingPlatform("");
		}
	};

	return (
		<div className="flex flex-col gap-4">
			<div>Chọn nền tảng</div>
			{configLoading ? (
				<div className="text-text-secondary text-sm">Đang kiểm tra cấu hình...</div>
			) : null}
			{error ? <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div> : null}
			<div className="grid grid-cols-2 gap-2">
				{
					platforms.map((platform) => {
						const key = platform.toLowerCase();
						const isEnabled = available[key] ?? true;
						return (
							<Button
								variant="soft"
								className="flex flex-col items-center gap-2 px-24 py-8 cursor-pointer h-fit"
								key={platform}
								onClick={() => handleConnect(platform)}
								disabled={loadingPlatform === platform || !isEnabled}
							>
								<img src={icons[platform.toLowerCase()]} alt={platform} className="w-12 h-12" />
								<div>
									{loadingPlatform === platform ? "Đang chuyển hướng..." : platform}
								</div>
								{!isEnabled ? (
									<span className="text-xs text-yellow-700">Thiếu cấu hình</span>
								) : null}
							</Button>
						);
					})
				}
			</div>
		</div>
	);
};