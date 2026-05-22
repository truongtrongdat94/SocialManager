import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { api } from "@/lib/api";
import { usePageInsights } from "../../hooks/usePageInsights";
import { ImpressionsChart } from "../../components/charts/ImpressionsChart";
import { FansGrowthChart } from "../../components/charts/FansGrowthChart";
import { EngagementChart } from "../../components/charts/EngagementChart";
import { ReactionsChart } from "../../components/charts/ReactionsChart";
import { PostPerformanceTable } from "../../components/charts/PostPerformanceTable";

type SocialAccount = {
	id: string;
	platform: string;
	externalAccountId: string;
	accountName: string;
};

export function Stats() {
	const [accounts, setAccounts] = useState<SocialAccount[]>([]);
	const [selectedPageId, setSelectedPageId] = useState<string>("");
	const [dateRange, setDateRange] = useState({
		since: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
		until: new Date().toISOString().split('T')[0]
	});
	const [posts, setPosts] = useState<any[]>([]);
	const [loadingPosts, setLoadingPosts] = useState(false);

	// Biểu đồ 1: Reach & Impressions (bỏ page_impressions deprecated)
	const impressionsData = usePageInsights({
		pageId: selectedPageId,
		metrics: ['page_impressions_unique', 'page_posts_impressions_organic', 'page_posts_impressions_paid'],
		since: dateRange.since,
		until: dateRange.until,
		period: 'day'
	});

	// Biểu đồ 2: Tăng trưởng Fans
	const fansGrowthData = usePageInsights({
		pageId: selectedPageId,
		metrics: [
			'page_fans',
			'page_fan_adds_unique',
			'page_fan_removes_unique',
			'page_daily_follows_unique'
		],
		since: dateRange.since,
		until: dateRange.until,
		period: 'day'
	});

	// Biểu đồ 3: Engagement (chỉ còn 1 metric)
	const engagementData = usePageInsights({
		pageId: selectedPageId,
		metrics: ['page_post_engagements'],
		since: dateRange.since,
		until: dateRange.until,
		period: 'day'
	});

	// Biểu đồ 4: Reactions (đầy đủ)
	const reactionsData = usePageInsights({
		pageId: selectedPageId,
		metrics: [
			'page_actions_post_reactions_like_total',
			'page_actions_post_reactions_love_total',
			'page_actions_post_reactions_wow_total',
			'page_actions_post_reactions_haha_total',
			'page_actions_post_reactions_sorry_total',
			'page_actions_post_reactions_anger_total'
		],
		since: dateRange.since,
		until: dateRange.until,
		period: 'day'
	});

	// Transform data cho các biểu đồ
	const impressionsChartData = useMemo(() => {
		if (!impressionsData.data) return [];
		return transformInsightsToChartData(impressionsData.data);
	}, [impressionsData.data]);

	const engagementChartData = useMemo(() => {
		if (!engagementData.data) return [];
		return transformInsightsToChartData(engagementData.data);
	}, [engagementData.data]);

	const fansGrowthChartData = useMemo(() => {
		if (!fansGrowthData.data) return [];
		return transformInsightsToChartData(fansGrowthData.data);
	}, [fansGrowthData.data]);

	const reactionsChartData = useMemo(() => {
		if (!reactionsData.data) return [];
		// Tổng hợp reactions từ tất cả các ngày
		const totals: Record<string, number> = {};
		reactionsData.data.forEach((insight: any) => {
			const type = insight.name.replace('page_actions_post_reactions_', '').replace('_total', '');
			const total = insight.values.reduce((sum: number, v: any) => sum + (typeof v.value === 'number' ? v.value : 0), 0);
			totals[type] = total;
		});
		return Object.entries(totals).map(([type, value]) => ({ type, value }));
	}, [reactionsData.data]);

	// Helper function để transform insights thành chart data
	function transformInsightsToChartData(insights: any[]): any[] {
		if (!insights || insights.length === 0) return [];
		
		const dateMap: Record<string, any> = {};
		
		insights.forEach(insight => {
			insight.values.forEach((v: any) => {
				const date = v.endTime || v.end_time;
				if (!dateMap[date]) {
					dateMap[date] = { date };
				}
				dateMap[date][insight.name] = typeof v.value === 'number' ? v.value : 0;
			});
		});
		
		return Object.values(dateMap).sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
	}

	// Validate and update date range
	const handleDateChange = (field: 'since' | 'until', value: string) => {
		const newRange = { ...dateRange, [field]: value };
		
		const sinceDate = new Date(newRange.since);
		const untilDate = new Date(newRange.until);
		
		if (sinceDate > untilDate) {
			toast.error("Ngày bắt đầu phải trước ngày kết thúc");
			return;
		}
		
		const diffTime = Math.abs(untilDate.getTime() - sinceDate.getTime());
		const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
		
		if (diffDays > 93) {
			toast.error("Khoảng thời gian tối đa là 93 ngày");
			return;
		}
		
		setDateRange(newRange);
	};

	// Load Facebook accounts
	useEffect(() => {
		let isMounted = true;

		const fetchAccounts = async () => {
			try {
				const response = await api.get("/api/social-accounts");
				
				if (!isMounted) return;
				
				const allAccounts = response.data.data || response.data;
				
				const facebookAccounts = allAccounts.filter(
					(acc: SocialAccount) => acc.platform === "FACEBOOK"
				);
				setAccounts(facebookAccounts);
				
				if (facebookAccounts.length > 0) {
					setSelectedPageId(facebookAccounts[0].externalAccountId);
				}
			} catch (error: any) {
				if (!isMounted) return;
				toast.error("Không thể tải danh sách tài khoản Facebook");
			}
		};

		fetchAccounts();
		
		return () => {
			isMounted = false;
		};
	}, []);

	// Biểu đồ 5: Load posts với insights (API khác)
	useEffect(() => {
		if (!selectedPageId) return;

		let isMounted = true;

		const fetchPosts = async () => {
			try {
				setLoadingPosts(true);
				const response = await api.get(`/api/posts/${selectedPageId}`, {
					params: { limit: 20 }
				});

				if (!isMounted) return;

				const postsData = response.data.data || [];
				
				// Fetch insights for each post
				// Note: Facebook only provides insights for posts that are:
				// - Published (not draft/scheduled)
				// - At least 24 hours old
				// - Have sufficient engagement
				const postsWithInsights = await Promise.all(
					postsData.map(async (post: any) => {
						try {
							// Encode post ID to handle special characters like underscore
							const encodedPostId = encodeURIComponent(post.id);
							const insightsResponse = await api.get(`/api/analytics/post/${encodedPostId}`, {
								params: { pageId: selectedPageId }
							});
							
							// Transform insights array to object
							const insightsArray = insightsResponse.data.data || [];
							const insights: any = {};
							
							insightsArray.forEach((insight: any) => {
								if (insight.name && insight.values && insight.values.length > 0) {
									insights[insight.name] = insight.values[0].value;
								}
							});
							
							return { ...post, insights };
						} catch (error: any) {
							// Silently skip posts without insights (too new, not enough engagement, etc.)
							// This is expected behavior for recent posts
							return post; // Return post without insights
						}
					})
				);

				if (!isMounted) return;
				setPosts(postsWithInsights);
			} catch (error: any) {
				if (!isMounted) return;
				console.error("Lỗi khi lấy posts:", error);
			} finally {
				if (isMounted) {
					setLoadingPosts(false);
				}
			}
		};

		fetchPosts();

		return () => {
			isMounted = false;
		};
	}, [selectedPageId]);

	const loading = impressionsData.loading || fansGrowthData.loading || engagementData.loading || reactionsData.loading;
	const hasError = impressionsData.error || fansGrowthData.error || engagementData.error || reactionsData.error;

	return (
		<div className="relative w-full overflow-hidden rounded-[30px] border border-sky-100 bg-gradient-to-br from-[#f2fbff] via-[#ecf8ff] to-[#def4ff] p-5 shadow-[0_12px_30px_rgba(56,146,183,0.15)] lg:p-7">
			<div className="pointer-events-none absolute -left-3 top-2 text-[64px] opacity-20 select-none">🐻‍❄️</div>
			<div className="pointer-events-none absolute -right-2 bottom-1 text-[60px] opacity-20 select-none">🐧</div>

			<div className="mb-8 rounded-[24px] border border-sky-100/80 bg-white/75 px-5 py-5 backdrop-blur-sm">
				<h1 className="text-3xl font-bold tracking-tight text-sky-800">Thống kê Facebook</h1>
				<p className="mt-2 text-base text-sky-700/80">
					Dashboard insights với 5 biểu đồ chuyên sâu
				</p>
			</div>

			{/* Filters */}
			<div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-3">
				<div>
					<label className="mb-2 block text-sm font-semibold text-sky-700">Chọn Facebook Page</label>
					<select
						value={selectedPageId}
						onChange={(e) => setSelectedPageId(e.target.value)}
						className="h-11 w-full rounded-xl border border-sky-200 bg-white px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400"
					>
						<option value="">-- Chọn Page --</option>
						{accounts.map((acc) => (
							<option key={acc.id} value={acc.externalAccountId}>
								{acc.accountName}
							</option>
						))}
					</select>
				</div>

				<div>
					<label className="mb-2 block text-sm font-semibold text-sky-700">Từ ngày</label>
					<input
						type="date"
						value={dateRange.since}
						onChange={(e) => handleDateChange('since', e.target.value)}
						max={dateRange.until}
						className="h-11 w-full rounded-xl border border-sky-200 bg-white px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400"
					/>
				</div>

				<div>
					<label className="mb-2 block text-sm font-semibold text-sky-700">Đến ngày</label>
					<input
						type="date"
						value={dateRange.until}
						onChange={(e) => handleDateChange('until', e.target.value)}
						min={dateRange.since}
						max={new Date().toISOString().split('T')[0]}
						className="h-11 w-full rounded-xl border border-sky-200 bg-white px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400"
					/>
				</div>
			</div>

			{loading ? (
				<div className="rounded-[24px] border border-sky-100 bg-white/90 p-5 text-sm text-sky-600">
					Đang tải dữ liệu thống kê...
				</div>
			) : !selectedPageId ? (
				<div className="rounded-[24px] border border-sky-100 bg-white/90 p-5 text-sm text-sky-600">
					Vui lòng chọn Facebook Page để xem thống kê.
				</div>
			) : hasError ? (
				<div className="rounded-[24px] border border-sky-100 bg-white/90 p-5 text-sm text-red-600">
					Lỗi: {impressionsData.error || engagementData.error || reactionsData.error}
				</div>
			) : (
				<div className="space-y-7">
					{/* Biểu đồ 1: Reach & Impressions */}
					<div className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
						<h2 className="mb-4 text-xl font-bold text-sky-800">1. Reach & Impressions theo thời gian</h2>
						<p className="mb-3 text-sm text-sky-600">⚠️ Metric page_impressions đã deprecated, chỉ hiển thị 3 metrics còn lại</p>
						{impressionsChartData.length > 0 ? (
							<ImpressionsChart data={impressionsChartData} />
						) : (
							<p className="text-sm text-sky-600">Chưa có dữ liệu</p>
						)}
					</div>

					{/* Biểu đồ 2: Tăng trưởng Fans */}
					<div className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
						<h2 className="mb-4 text-xl font-bold text-sky-800">2. Tăng trưởng Fans</h2>
						{fansGrowthChartData.length > 0 ? (
							<FansGrowthChart data={fansGrowthChartData} />
						) : (
							<p className="text-sm text-sky-600">Chưa có dữ liệu</p>
						)}
					</div>

					{/* Biểu đồ 3: Engagement */}
					<div className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
						<h2 className="mb-4 text-xl font-bold text-sky-800">3. Engagement tổng hợp</h2>
						<p className="mb-3 text-sm text-sky-600">⚠️ Metric page_consumptions_unique đã deprecated</p>
						{engagementChartData.length > 0 ? (
							<EngagementChart data={engagementChartData} />
						) : (
							<p className="text-sm text-sky-600">Chưa có dữ liệu</p>
						)}
					</div>

					{/* Biểu đồ 4: Reactions */}
					<div className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
						<h2 className="mb-4 text-xl font-bold text-sky-800">4. Phân tích Reactions</h2>
						{reactionsChartData.length > 0 ? (
							<ReactionsChart data={reactionsChartData} />
						) : (
							<p className="text-sm text-sky-600">Chưa có dữ liệu</p>
						)}
					</div>

					{/* Biểu đồ 5: Hiệu suất bài viết */}
					<div className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
						<h2 className="mb-4 text-xl font-bold text-sky-800">5. Hiệu suất từng bài viết</h2>
						{loadingPosts ? (
							<p className="text-sm text-sky-600">Đang tải bài viết...</p>
						) : posts.length > 0 ? (
							<PostPerformanceTable posts={posts} />
						) : (
							<p className="text-sm text-sky-600">Chưa có bài viết</p>
						)}
					</div>
				</div>
			)}
		</div>
	);
}
