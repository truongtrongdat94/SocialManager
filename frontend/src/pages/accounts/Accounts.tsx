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

	const fetchAccounts = async () => {
		try {
			setLoading(true);
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

	return (
		<div className="relative w-full overflow-hidden rounded-[30px] border border-sky-100 bg-gradient-to-br from-[#f2fbff] via-[#ecf8ff] to-[#def4ff] p-5 shadow-[0_12px_30px_rgba(56,146,183,0.15)] lg:p-7">
			<div className="pointer-events-none absolute -left-3 top-2 text-[64px] opacity-20 select-none">🐻‍❄️</div>
			<div className="pointer-events-none absolute -right-2 bottom-1 text-[60px] opacity-20 select-none">🐧</div>

			<div className="mb-8 flex flex-wrap items-start justify-between gap-4 rounded-[24px] border border-sky-100/80 bg-white/75 px-5 py-5 backdrop-blur-sm">
				<div>
					<h1 className="text-3xl font-bold tracking-tight text-sky-800">Tài khoản kết nối</h1>
					<p className="mt-2 text-base text-sky-700/80">Quản lý tài khoản social đã kết nối vào hệ thống.</p>
				</div>

				<div className="flex flex-wrap gap-2">
					{PLATFORM_OPTIONS.map((platform) => (
						<button
							key={platform}
							type="button"
							onClick={() => connectPlatform(platform)}
							className="h-11 rounded-full border border-sky-200 bg-white px-5 text-sm font-semibold text-sky-700 transition hover:border-sky-300 hover:bg-sky-50"
						>
							Kết nối {platform}
						</button>
					))}
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
		</div>
	);
}
