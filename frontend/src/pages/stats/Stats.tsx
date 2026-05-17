<<<<<<< HEAD
import { useEffect, useMemo, useState } from "react";
import axiosInstance from "@/libs/axios";

type Platform = "FACEBOOK" | "INSTAGRAM" | "THREADS" | "TIKTOK";

type MonitorSummary = {
	pending: number;
	processing: number;
	posted: number;
	failed: number;
};

type PostHistoryItem = {
	id: string;
	socialAccountId: string | null;
	socialAccountName: string | null;
	platform: Platform | null;
	content: string | null;
	mediaUrl: string | null;
	scheduledTime: string | null;
	status: string | null;
	publishedPostId: string | null;
	publishedPostUrl: string | null;
	errorMessage: string | null;
	retryCount: number | null;
	lastAttemptAt: string | null;
	autoPilot: boolean | null;
	createdAt: string | null;
};

type PagedHistoryResponse = {
	items: PostHistoryItem[];
	total: number;
	page: number;
	size: number;
	hasNext: boolean;
};

const getPublishedPostHref = (
	platform: Platform | null,
	publishedPostId?: string | null,
	publishedPostUrl?: string | null,
) => {
	if (publishedPostUrl && publishedPostUrl.trim()) {
		return publishedPostUrl;
	}

	if (!publishedPostId || !publishedPostId.trim()) {
		return "";
	}

	const safeId = publishedPostId.trim();
	switch (platform) {
		case "FACEBOOK":
			return `https://www.facebook.com/${safeId}`;
		case "INSTAGRAM":
			return `https://www.instagram.com/p/${safeId}/`;
		case "THREADS":
			return `https://www.threads.net/post/${safeId}`;
		case "TIKTOK":
			return `https://www.tiktok.com/@/video/${safeId}`;
		default:
			return "";
	}
};

export const Stats = () => {
	const [summary, setSummary] = useState<MonitorSummary | null>(null);
	const [history, setHistory] = useState<PostHistoryItem[]>([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState("");

	const counts = useMemo(() => {
		return summary ?? { pending: 0, processing: 0, posted: 0, failed: 0 };
	}, [summary]);

	const loadStats = async () => {
		setLoading(true);
		setError("");
		try {
			const [summaryRes, historyRes] = await Promise.all([
				axiosInstance.get<MonitorSummary>("/posts/monitor/summary"),
				axiosInstance.get<{ data: PagedHistoryResponse }>("/posts/history?page=0&size=20"),
			]);
			setSummary(summaryRes.data);
			setHistory(historyRes.data.data?.items ?? []);
		} catch (err: any) {
			setError(err?.response?.data?.message || "Không thể tải thống kê lúc này.");
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		loadStats();
	}, []);

	useEffect(() => {
		const interval = setInterval(() => {
			loadStats();
		}, 10000);
		return () => clearInterval(interval);
	}, []);

	return (
		<div className="flex h-full flex-col gap-4">
			<div className="flex items-center justify-between">
				<div>
					<div className="text-2xl font-bold">Thống kê</div>
					<div className="text-text-secondary text-sm">Theo dõi tình trạng đăng bài và lịch sử đăng.</div>
				</div>
				<button
					onClick={() => loadStats()}
					disabled={loading}
					className="rounded-md border border-border px-3 py-2 text-sm hover:bg-surface-secondary"
				>
					{loading ? "Đang tải..." : "Làm mới"}
				</button>
			</div>

			{error && <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}

			<div className="grid grid-cols-1 gap-3 md:grid-cols-4">
				<div className="rounded-xl border border-border bg-surface-primary p-4">
					<div className="text-sm text-text-secondary">Đang chờ</div>
					<div className="text-2xl font-bold">{counts.pending}</div>
				</div>
				<div className="rounded-xl border border-border bg-surface-primary p-4">
					<div className="text-sm text-text-secondary">Đang xử lý</div>
					<div className="text-2xl font-bold">{counts.processing}</div>
				</div>
				<div className="rounded-xl border border-border bg-surface-primary p-4">
					<div className="text-sm text-text-secondary">Đã đăng</div>
					<div className="text-2xl font-bold">{counts.posted}</div>
				</div>
				<div className="rounded-xl border border-border bg-surface-primary p-4">
					<div className="text-sm text-text-secondary">Thất bại</div>
					<div className="text-2xl font-bold">{counts.failed}</div>
				</div>
			</div>

			<div className="rounded-xl border border-border bg-surface-primary">
				<div className="border-b border-border px-4 py-3 font-semibold flex items-center justify-between">
					<span>Lịch sử đăng bài</span>
					<span className="text-xs text-text-secondary">Tự động cập nhật mỗi 10 giây</span>
				</div>
				<div className="max-h-[520px] overflow-y-auto">
					{loading ? (
						<div className="px-4 py-6 text-sm text-text-secondary">Đang tải...</div>
					) : history.length === 0 ? (
						<div className="px-4 py-6 text-sm text-text-secondary">Chưa có lịch sử.</div>
					) : (
						history.map((item) => {
							const href = getPublishedPostHref(item.platform, item.publishedPostId, item.publishedPostUrl);
							return (
								<div key={item.id} className="border-b border-border px-4 py-4 last:border-b-0">
									<div className="flex flex-wrap items-center justify-between gap-2">
										<div className="font-semibold">{item.socialAccountName ?? "(Không rõ tài khoản)"}</div>
										<div className="text-sm text-text-secondary">{item.status ?? ""}</div>
									</div>
									<div className="mt-2 text-sm text-text-secondary">{item.content ?? ""}</div>
									<div className="mt-2 flex flex-wrap items-center gap-4 text-xs text-text-secondary">
										<span>Lên lịch: {item.scheduledTime ?? ""}</span>
										{item.lastAttemptAt && <span>Lần thử: {item.lastAttemptAt}</span>}
										{item.errorMessage && <span className="text-red-600">{item.errorMessage}</span>}
									</div>
									{href ? (
										<a href={href} target="_blank" rel="noreferrer" className="mt-2 inline-block text-sm text-accent">
											Mở bài đăng
										</a>
									) : null}
								</div>
							);
						})
					)}
				</div>
			</div>
		</div>
	);
};
=======

export const Stats = () => {
	return <div>Stats</div>
}
>>>>>>> upstream/dev
