import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { api, type ApiResponse, getApiErrorMessage } from "@/lib/api";

type ScheduledPostHistoryItem = {
	id: string;
	caption: string;
	mediaUrls: string[];
	scheduledTime: string;
	status: string;
	publishedPostId: string | null;
		publishedPostUrl?: string | null;
	errorMessage: string | null;
	createdAt: string;
	accountName: string | null;
	accountPlatform: string | null;
};

export function PostHistory() {
	const [items, setItems] = useState<ScheduledPostHistoryItem[]>([]);
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		const fetchHistory = async () => {
			try {
				setLoading(true);
				const response = await api.get<ApiResponse<ScheduledPostHistoryItem[]>>("/api/social-accounts/scheduled-posts");
				setItems(Array.isArray(response.data.data) ? response.data.data : []);
			} catch (error) {
				toast.error(getApiErrorMessage(error));
			} finally {
				setLoading(false);
			}
		};

		void fetchHistory();
	}, []);

	return (
		<div className="relative w-full overflow-hidden rounded-[30px] border border-sky-100 bg-gradient-to-br from-[#f2fbff] via-[#ecf8ff] to-[#def4ff] p-5 shadow-[0_12px_30px_rgba(56,146,183,0.15)] lg:p-7">
			<div className="pointer-events-none absolute -left-3 top-2 text-[64px] opacity-20 select-none">🐻‍❄️</div>
			<div className="pointer-events-none absolute -right-2 bottom-1 text-[60px] opacity-20 select-none">🐧</div>

			<div className="mb-8 rounded-[24px] border border-sky-100/80 bg-white/75 px-5 py-5 backdrop-blur-sm">
				<h1 className="text-3xl font-bold tracking-tight text-sky-800">Lịch sử bài đăng</h1>
				<p className="mt-2 text-base text-sky-700/80">Theo dõi các bài đã lên lịch, trạng thái và lỗi nếu có.</p>
			</div>

			{loading ? (
				<div className="rounded-[24px] border border-sky-100 bg-white/90 p-5 text-sm text-sky-600">Đang tải lịch sử...</div>
			) : items.length === 0 ? (
				<div className="rounded-[24px] border border-sky-100 bg-white/90 p-5 text-sm text-sky-600">Chưa có bài đăng nào được lên lịch.</div>
			) : (
				<div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
					{items.map((item) => (
						<article key={item.id} className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
							<div className="flex items-start justify-between gap-4">
								<div>
									<p className="text-xs uppercase tracking-wide text-sky-500">{item.accountPlatform ?? "FACEBOOK"}</p>
									<h2 className="mt-1 text-lg font-semibold text-sky-900">{item.accountName ?? "Tài khoản không rõ"}</h2>
								</div>
								<span className="rounded-full border border-sky-200 bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">{item.status}</span>
							</div>

							<p className="mt-4 text-sm whitespace-pre-wrap text-sky-800">{item.caption}</p>

							<div className="mt-4 grid gap-2 text-sm text-sky-700">
								<p><span className="font-semibold">Scheduled:</span> {new Date(item.scheduledTime).toLocaleString()}</p>
								<p><span className="font-semibold">Created:</span> {new Date(item.createdAt).toLocaleString()}</p>
								{item.publishedPostId ? <p><span className="font-semibold">Published ID:</span> {item.publishedPostId}</p> : null}
								{item.publishedPostUrl ? <p><span className="font-semibold">Link:</span> <a href={item.publishedPostUrl} target="_blank" rel="noreferrer" className="text-sky-600">Mở bài đã đăng</a></p> : null}
								{item.errorMessage ? <p className="text-rose-600"><span className="font-semibold">Error:</span> {item.errorMessage}</p> : null}
							</div>

							{item.mediaUrls.length > 0 ? (
								<div className="mt-4 flex flex-wrap gap-2">
									{item.mediaUrls.map((url) => (
										<a key={url} href={url} target="_blank" rel="noreferrer" className="rounded-full border border-sky-200 bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">
											Media
										</a>
									))}
								</div>
							) : null}
						</article>
					))}
				</div>
			)}
		</div>
	);
}
