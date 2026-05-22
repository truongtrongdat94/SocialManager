import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { api, type ApiResponse, type SocialAccountDto, getApiErrorMessage } from "@/lib/api";

const PLATFORM_OPTIONS: Array<"facebook" | "instagram" | "threads" | "tiktok"> = [
	"facebook",
	"instagram",
	"threads",
	"tiktok",
];

export function Accounts() {
	const [rows, setRows] = useState<SocialAccountDto[]>([]);
	const [loading, setLoading] = useState(false);
	const [deletingId, setDeletingId] = useState<string | null>(null);
	const [cloudinaryConfigured, setCloudinaryConfigured] = useState(false);
	const [cloudinaryValid, setCloudinaryValid] = useState(false);
	const [cloudinaryCloudName, setCloudinaryCloudName] = useState<string | null>(null);
	const [cloudinaryReason, setCloudinaryReason] = useState<string | null>(null);

	const [showCloudinaryForm, setShowCloudinaryForm] = useState(false);
	const [cloudName, setCloudName] = useState("");
	const [apiKey, setApiKey] = useState("");
	const [apiSecret, setApiSecret] = useState("");

	// Facebook / Meta config fields (frontend admin panel)
	const [showMetaForm, setShowMetaForm] = useState(false);
	const [metaAppId, setMetaAppId] = useState("");
	const [metaAppSecret, setMetaAppSecret] = useState("");
	const [metaRedirectUri, setMetaRedirectUri] = useState("http://localhost:8080/api/social-accounts/callback/facebook");
	const [metaHasSecret, setMetaHasSecret] = useState(false);
	const [metaTesting, setMetaTesting] = useState(false);
	const [metaReadyToConnect, setMetaReadyToConnect] = useState(false);

	const saveMetaConfig = async () => {
		try {
			const payload = { appId: metaAppId, appSecret: metaAppSecret, redirectUri: metaRedirectUri };
			await api.post("/api/admin/config/meta", payload);
			toast.success("Meta/Facebook cấu hình đã lưu");
			setMetaHasSecret(Boolean(metaAppSecret));
			setMetaReadyToConnect(true);
		} catch (err) {
			toast.error(getApiErrorMessage(err));
		}
	};

	const testMetaConfig = async () => {
		try {
			setMetaTesting(true);
			const resp = await api.post<ApiResponse<string>>("/api/admin/config/meta/test");
			const url = resp.data?.data;
			if (typeof url === "string" && url.startsWith("http")) {
				toast.success("Cấu hình hợp lệ — OAuth URL tạo được");
				window.open(url, "_blank");
			} else {
				toast.error("Cấu hình không hợp lệ");
			}
		} catch (err) {
			toast.error(getApiErrorMessage(err));
		} finally {
			setMetaTesting(false);
		}
	};

	const fetchAccounts = async () => {
		try {
			setLoading(true);
			const cloudinaryResponse = await api.get<ApiResponse<{ configured: boolean; valid?: boolean; cloudName?: string; reason?: string }>>("/api/cloudinary");
			setCloudinaryConfigured(Boolean(cloudinaryResponse.data.data?.configured));
			setCloudinaryValid(Boolean(cloudinaryResponse.data.data?.valid));
			setCloudinaryCloudName(cloudinaryResponse.data.data?.cloudName ?? null);
			setCloudinaryReason(cloudinaryResponse.data.data?.reason ?? null);

			// Try to fetch existing Meta config (may 401 if not authenticated)
			try {
				const metaResp = await api.get<ApiResponse<{ appId?: string | null; redirectUri?: string | null; hasSecret?: boolean }>>("/api/admin/config/meta");
				const meta = metaResp.data?.data;
				if (meta) {
					setMetaAppId(meta.appId ?? "");
					setMetaRedirectUri(meta.redirectUri ?? metaRedirectUri);
					setMetaHasSecret(Boolean(meta.hasSecret));
					setMetaReadyToConnect(Boolean(meta.appId && meta.redirectUri && meta.hasSecret));
				}
			} catch (err) {
				// ignore if unauthenticated or forbidden
			}
			const response = await api.get<ApiResponse<SocialAccountDto[]>>("/api/social-accounts");
			setRows(Array.isArray(response.data.data) ? response.data.data : []);
		} catch (error) {
			toast.error(getApiErrorMessage(error));
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		void fetchAccounts();
	}, []);

	const platformLabel = useMemo(
		() => ({
			FACEBOOK: "Facebook",
			INSTAGRAM: "Instagram",
			THREADS: "Threads",
			TIKTOK: "TikTok",
		}),
		[],
	);

	const connectPlatform = async (platform: (typeof PLATFORM_OPTIONS)[number]) => {
		try {
			const response = await api.get<ApiResponse<string>>(`/api/social-accounts/connect/${platform.toUpperCase()}`);
			const url = response.data?.data;
			if (typeof url === "string" && url.startsWith("http")) {
				window.location.href = url;
				return;
			}
			toast("Không lấy được OAuth URL hợp lệ.");
		} catch (error) {
			toast.error(getApiErrorMessage(error));
		}
	};

	const openFacebookConfig = () => {
		setShowMetaForm(true);
	};

	const handleDelete = async (id: string) => {
		try {
			setDeletingId(id);
			await api.delete(`/api/social-accounts/${id}`);
			toast.success("Đã xoá tài khoản.");
			await fetchAccounts();
		} catch (error) {
			toast.error(getApiErrorMessage(error));
		} finally {
			setDeletingId(null);
		}
	};

	const saveCloudinary = async () => {
		try {
			const payload = { cloudName, apiKey, apiSecret };
			await api.post("/api/cloudinary", payload);
			toast.success("Cloudinary đã được lưu");
			setShowCloudinaryForm(false);
		} catch (err) {
			toast.error(getApiErrorMessage(err));
		}
	};

	return (
		<div className="relative w-full overflow-hidden rounded-[30px] border border-sky-100 bg-gradient-to-br from-[#f2fbff] via-[#ecf8ff] to-[#def4ff] p-5 shadow-[0_12px_30px_rgba(56,146,183,0.15)] lg:p-7">
			<div className="pointer-events-none absolute -left-3 top-2 text-[64px] opacity-20 select-none">🐻‍❄️</div>
			<div className="pointer-events-none absolute -right-2 bottom-1 text-[60px] opacity-20 select-none">🐧</div>

			<div className="mb-8 flex flex-wrap items-start justify-between gap-4 rounded-[24px] border border-sky-100/80 bg-white/75 px-5 py-5 backdrop-blur-sm">
				<div>
					<h1 className="text-3xl font-bold tracking-tight text-sky-800">Tài khoản kết nối</h1>
					<p className="mt-2 text-base text-sky-700/80">Quản lý tài khoản social đã kết nối vào hệ thống.</p>
					<div className="mt-3 inline-flex items-center gap-2 rounded-full border border-sky-200 bg-white px-3 py-1 text-xs font-semibold text-sky-700">
						<span className={`h-2.5 w-2.5 rounded-full ${cloudinaryConfigured && cloudinaryValid ? "bg-emerald-500" : "bg-rose-500"}`} />
						{cloudinaryConfigured && cloudinaryValid
							? `Cloudinary đã kết nối${cloudinaryCloudName ? `: ${cloudinaryCloudName}` : ""}`
							: cloudinaryConfigured
								? `Cloudinary cấu hình lỗi${cloudinaryReason ? `: ${cloudinaryReason}` : ""}`
								: "Cloudinary chưa kết nối"}
					</div>
				</div>

				<div className="flex flex-wrap gap-2">
					{PLATFORM_OPTIONS.map((platform) => (
						platform === "facebook" ? (
							<button
								key={platform}
								type="button"
								onClick={openFacebookConfig}
								className="h-11 rounded-full border border-sky-200 bg-white px-5 text-sm font-semibold text-sky-700 transition hover:border-sky-300 hover:bg-sky-50"
							>
								Kết nối Facebook
							</button>
						) : (
							<button
								key={platform}
								type="button"
								onClick={() => connectPlatform(platform)}
								className="h-11 rounded-full border border-sky-200 bg-white px-5 text-sm font-semibold text-sky-700 transition hover:border-sky-300 hover:bg-sky-50"
							>
								Kết nối {platform}
							</button>
						)
					))}

					<button
						type="button"
						onClick={() => setShowCloudinaryForm((s) => !s)}
						className="h-11 rounded-full border border-sky-200 bg-white px-5 text-sm font-semibold text-sky-700 transition hover:border-sky-300 hover:bg-sky-50"
					>
						Kết nối Cloudinary
					</button>

				</div>
			</div>

			<section className="overflow-hidden rounded-[28px] border border-sky-100 bg-white/90 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
				<div className="border-b border-sky-100 px-6 py-4">
					<p className="text-base font-semibold text-sky-700">Danh sách tài khoản</p>
				</div>

				{loading ? (
					<div className="px-6 py-7 text-sm text-sky-600">Đang tải dữ liệu...</div>
				) : rows.length === 0 ? (
					<div className="px-6 py-7 text-sm text-sky-600">Chưa có tài khoản nào.</div>
				) : (
					<div className="overflow-x-auto">
						<table className="w-full min-w-[760px] border-collapse">
							<thead>
								<tr className="border-b border-sky-100 text-left text-xs uppercase tracking-wide text-sky-500">
									<th className="px-6 py-3.5">Platform</th>
									<th className="px-6 py-3.5">Account Name</th>
									<th className="px-6 py-3.5">Account ID</th>
									<th className="px-6 py-3.5">Created</th>
									<th className="px-6 py-3.5">Action</th>
								</tr>
							</thead>
							<tbody>
								{rows.map((item) => (
									<tr key={item.id} className="border-b border-sky-100 text-sm">
										<td className="px-6 py-4.5 text-sky-800">{platformLabel[item.platform]}</td>
										<td className="px-6 py-4.5 font-medium text-sky-900">{item.accountName}</td>
										<td className="px-6 py-4.5 text-sky-700">{item.accountId}</td>
										<td className="px-6 py-4.5 text-sky-700">
											{item.createdAt ? new Date(item.createdAt).toLocaleString() : "N/A"}
										</td>
										<td className="px-6 py-4.5">
											<button
												type="button"
												onClick={() => handleDelete(item.id)}
												disabled={deletingId === item.id}
												className="h-10 rounded-full border border-red-200 bg-white px-4 text-xs font-semibold text-red-600 transition hover:bg-red-50 disabled:opacity-60"
											>
												{deletingId === item.id ? "Đang xoá..." : "Xoá"}
											</button>
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				)}
			</section>

			{showCloudinaryForm && (
				<section className="mt-6 rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] lg:p-7">
					<h3 className="mb-3 text-lg font-semibold text-sky-800">Kết nối Cloudinary</h3>
					<div className="space-y-3">
						<input placeholder="Cloud name" value={cloudName} onChange={(e) => setCloudName(e.target.value)} className="w-full rounded-xl border px-3 py-2" />
						<input placeholder="API Key" value={apiKey} onChange={(e) => setApiKey(e.target.value)} className="w-full rounded-xl border px-3 py-2" />
						<input placeholder="API Secret" value={apiSecret} onChange={(e) => setApiSecret(e.target.value)} className="w-full rounded-xl border px-3 py-2" />
						<div className="flex gap-2">
							<button onClick={saveCloudinary} className="h-10 rounded-full bg-sky-600 px-4 text-white">Lưu</button>
							<button onClick={() => setShowCloudinaryForm(false)} className="h-10 rounded-full border px-4">Huỷ</button>
						</div>
					</div>
				</section>
			)}

			{showMetaForm && (
				<section className="mt-6 rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] lg:p-7">
					<h3 className="mb-3 text-lg font-semibold text-sky-800">Cấu hình Meta / Facebook</h3>
					<div className="space-y-3">
						<input placeholder="App ID" value={metaAppId} onChange={(e) => setMetaAppId(e.target.value)} className="w-full rounded-xl border px-3 py-2" />
						<input placeholder="App Secret" type="password" value={metaAppSecret} onChange={(e) => setMetaAppSecret(e.target.value)} className="w-full rounded-xl border px-3 py-2" />
						<input placeholder="Redirect URI" value={metaRedirectUri} onChange={(e) => setMetaRedirectUri(e.target.value)} className="w-full rounded-xl border px-3 py-2" />
						<div className="text-sm text-sky-700">{metaHasSecret ? "Secret đã lưu" : "Secret chưa lưu"}</div>
						<div className="flex gap-2">
							<button onClick={saveMetaConfig} className="h-10 rounded-full bg-sky-600 px-4 text-white">Lưu</button>
							{metaReadyToConnect ? (
								<button onClick={() => connectPlatform("facebook")} className="h-10 rounded-full bg-emerald-600 px-4 text-white">
									Kết nối Facebook
								</button>
							) : (
								<button onClick={testMetaConfig} disabled={metaTesting} className="h-10 rounded-full border px-4">{metaTesting ? "Đang kiểm tra..." : "Kiểm tra"}</button>
							)}
							<button onClick={() => setShowMetaForm(false)} className="h-10 rounded-full border px-4">Huỷ</button>
						</div>
						<p className="text-xs text-sky-500">Sau khi lưu cấu hình hợp lệ, nút kết nối Facebook sẽ xuất hiện ở đây.</p>
					</div>
				</section>
			)}
		</div>
	);
}

