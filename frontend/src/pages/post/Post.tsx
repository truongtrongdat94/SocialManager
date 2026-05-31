import { useMemo, useState, useEffect } from "react";
import { useLocation } from "react-router";
import toast from "react-hot-toast";
import { api } from "../../lib/api";

type IncomingAiState = {
	caption?: string;
	imageUrl?: string;
};

type SocialAccount = {
	id: string;
	platform: string;
	externalAccountId: string;
	accountName: string;
	profilePictureUrl: string;
};

export function Post() {
	const location = useLocation();
	const incoming = (location.state as IncomingAiState | null) ?? null;

	const [caption, setCaption] = useState(incoming?.caption ?? "");
	const [imageUrl, setImageUrl] = useState(incoming?.imageUrl ?? "");
	const [postMode, setPostMode] = useState<"now" | "schedule">("now");
	const [scheduledAt, setScheduledAt] = useState("");
	const [isPosting, setIsPosting] = useState(false);
	
	// Danh sách tài khoản và tài khoản được chọn
	const [accounts, setAccounts] = useState<SocialAccount[]>([]);
	const [selectedPageId, setSelectedPageId] = useState<string>("");
	
	// Danh sách bài đã lên lịch
	const [scheduledPosts, setScheduledPosts] = useState<any[]>([]);
	const [showScheduled, setShowScheduled] = useState(false);

	const canPublish = useMemo(() => {
		return caption.trim().length > 0 && selectedPageId.length > 0;
	}, [caption, selectedPageId]);

	// Load scheduled posts khi chọn page
	const loadScheduledPosts = async (pageId: string) => {
		if (!pageId) return;
		
		try {
			const response = await api.get(`/api/posts/${pageId}/scheduled`);
			const posts = response.data.data || [];
			console.log('📅 Scheduled posts response:', posts);
			setScheduledPosts(posts);
		} catch (error) {
			console.error("Lỗi khi load bài đã lên lịch:", error);
		}
	};

	// Load danh sách tài khoản Facebook
	useEffect(() => {
		let isMounted = true;

		const fetchAccounts = async () => {
			try {
				const response = await api.get("/api/social-accounts");
				
				if (!isMounted) return;
				
				// API returns ApiResponse<List<SocialAccountDto>> format: { data: [...], success: true, message: "..." }
				const allAccounts = response.data.data || response.data;
				
				const facebookAccounts = allAccounts.filter(
					(acc: SocialAccount) => acc.platform === "FACEBOOK"
				);
				
				setAccounts(facebookAccounts);
				
				// Auto-select first account
				if (facebookAccounts.length > 0 && facebookAccounts[0].externalAccountId) {
					setSelectedPageId(facebookAccounts[0].externalAccountId);
					loadScheduledPosts(facebookAccounts[0].externalAccountId);
				}
			} catch (error: any) {
				if (!isMounted) return;
				
				console.error("Lỗi khi load tài khoản:", error);
				toast.error("Không thể tải danh sách tài khoản Facebook");
			}
		};

		fetchAccounts();
		
		return () => {
			isMounted = false;
		};
	}, []);

	const handlePublish = async () => {
		if (!canPublish) {
			toast.error("Vui lòng nhập caption và chọn tài khoản trước khi đăng.");
			return;
		}

		if (postMode === "schedule" && !scheduledAt) {
			toast.error("Vui lòng chọn thời gian lên lịch.");
			return;
		}

		setIsPosting(true);

		try {
			// Convert datetime-local to Unix timestamp (seconds)
			const scheduledTimestamp = postMode === "schedule" 
				? Math.floor(new Date(scheduledAt).getTime() / 1000) 
				: null;

			if (imageUrl && imageUrl.trim().length > 0) {
				// Đăng ảnh
				await api.post(`/api/posts/${selectedPageId}/photo`, {
					photoUrl: imageUrl,
					caption: caption,
					scheduledPublishTime: scheduledTimestamp
				});
			} else {
				// Đăng text
				await api.post(`/api/posts/${selectedPageId}`, {
					message: caption,
					scheduledPublishTime: scheduledTimestamp
				});
			}

			toast.success(
				postMode === "now"
					? "Đã đăng bài thành công!"
					: `Đã lên lịch đăng lúc ${new Date(scheduledAt).toLocaleString("vi-VN")}`
			);

			// Reset form
			setCaption("");
			setImageUrl("");
			setScheduledAt("");
			setPostMode("now");
			
			// Reload scheduled posts
			if (postMode === "schedule") {
				loadScheduledPosts(selectedPageId);
			}
		} catch (error: any) {
			console.error("Lỗi khi đăng bài:", error);
			const errorMessage = error.response?.data?.message || error.message || "Có lỗi xảy ra";
			toast.error("Lỗi khi đăng bài: " + errorMessage);
		} finally {
			setIsPosting(false);
		}
	};

	return (
		<div className="relative w-full overflow-hidden rounded-[30px] border border-sky-100 bg-gradient-to-br from-[#f2fbff] via-[#ecf8ff] to-[#def4ff] p-5 shadow-[0_12px_30px_rgba(56,146,183,0.15)] lg:p-7">
			<div className="pointer-events-none absolute -left-3 top-2 text-[64px] opacity-20 select-none">🐻‍❄️</div>
			<div className="pointer-events-none absolute -right-2 bottom-1 text-[60px] opacity-20 select-none">🐧</div>

			<div className="mb-8 rounded-[24px] border border-sky-100/80 bg-white/75 px-5 py-5 backdrop-blur-sm">
				<h1 className="text-3xl font-bold tracking-tight text-sky-800">Đăng bài</h1>
				<p className="mt-2 text-base text-sky-700/80">
					Đăng bài lên Facebook Page với khả năng lên lịch tự động.
				</p>
			</div>

			<div className="grid grid-cols-1 gap-7 2xl:grid-cols-2">
				<section className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] lg:p-7">
					<h2 className="mb-5 text-xl font-bold text-sky-800">Nội dung</h2>

					<div className="space-y-4">
						<div>
							<label className="mb-2 block text-sm font-semibold text-sky-700">Chọn Facebook Page</label>
							<select
								value={selectedPageId}
								onChange={(e) => {
									setSelectedPageId(e.target.value);
									loadScheduledPosts(e.target.value);
								}}
								className="h-11 w-full rounded-xl border border-sky-200 bg-[#f7fcff] px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
							>
								<option value="">-- Chọn Page --</option>
								{accounts.map((acc) => (
									<option key={acc.id} value={acc.externalAccountId}>
										{acc.accountName} {acc.externalAccountId ? `(${acc.externalAccountId})` : '(No Page ID)'}
									</option>
								))}
							</select>
							{accounts.length > 0 && !accounts[0].externalAccountId && (
								<p className="mt-2 text-sm text-red-600">
									⚠️ Tài khoản không có Page ID. Vui lòng xóa và kết nối lại.
								</p>
							)}
						</div>

						<div>
							<label className="mb-2 block text-sm font-semibold text-sky-700">Caption</label>
							<textarea
								rows={8}
								value={caption}
								onChange={(e) => setCaption(e.target.value)}
								className="w-full rounded-2xl border border-sky-200 bg-[#f7fcff] px-4 py-3 text-[15px] text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
								placeholder="Nhập nội dung bài đăng..."
							/>
						</div>

						<div>
							<label className="mb-2 block text-sm font-semibold text-sky-700">Image URL (tùy chọn)</label>
							<input
								value={imageUrl}
								onChange={(e) => setImageUrl(e.target.value)}
								className="h-11 w-full rounded-xl border border-sky-200 bg-[#f7fcff] px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
								placeholder="https://..."
							/>
						</div>
					</div>
				</section>

				<section className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] lg:p-7">
					<h2 className="mb-5 text-xl font-bold text-sky-800">Preview & Publish</h2>

					<div className="space-y-4">
						{/* Facebook-style Preview */}
						<div className="overflow-hidden rounded-2xl border border-sky-200 bg-white">
							{/* Page Header */}
							<div className="flex items-center gap-3 border-b border-gray-200 p-4">
								<div className="h-10 w-10 rounded-full bg-gradient-to-br from-sky-400 to-blue-500 flex items-center justify-center text-white font-bold">
									{accounts.find(acc => acc.externalAccountId === selectedPageId)?.accountName?.charAt(0) || 'P'}
								</div>
								<div>
									<div className="font-semibold text-gray-900 text-sm">
										{accounts.find(acc => acc.externalAccountId === selectedPageId)?.accountName || 'Chọn Page'}
									</div>
									<div className="text-xs text-gray-500">Vừa xong · 🌍</div>
								</div>
							</div>
							
							{/* Caption */}
							{caption && (
								<div className="px-4 py-3 text-sm text-gray-800 whitespace-pre-wrap">
									{caption}
								</div>
							)}
							
							{/* Image */}
							{imageUrl ? (
								<img src={imageUrl} alt="Post preview" className="w-full max-h-[400px] object-cover" />
							) : (
								<div className="flex h-[200px] items-center justify-center bg-gray-50 text-sm text-gray-400">
									{caption ? 'Bài viết text' : 'Chưa có nội dung'}
								</div>
							)}
						</div>

						<div className="rounded-2xl border border-sky-200 bg-[#f8fdff] p-5">
							<p className="mb-3 text-sm font-semibold text-sky-700">Chế độ đăng</p>
							<div className="flex gap-2">
								<button
									type="button"
									onClick={() => setPostMode("now")}
									className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${
										postMode === "now"
											? "border-sky-500 bg-sky-500 text-white"
											: "border-sky-200 bg-white text-sky-700 hover:bg-sky-50"
									}`}
								>
									Đăng ngay
								</button>
								<button
									type="button"
									onClick={() => setPostMode("schedule")}
									className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${
										postMode === "schedule"
											? "border-sky-500 bg-sky-500 text-white"
											: "border-sky-200 bg-white text-sky-700 hover:bg-sky-50"
									}`}
								>
									Lên lịch
								</button>
							</div>

							{postMode === "schedule" && (
								<div className="mt-3">
									<input
										type="datetime-local"
										value={scheduledAt}
										onChange={(e) => setScheduledAt(e.target.value)}
										className="h-11 w-full rounded-xl border border-sky-200 bg-white px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
									/>
								</div>
							)}
						</div>

						<button
							type="button"
							onClick={handlePublish}
							disabled={!canPublish || isPosting}
							className="h-12 w-full rounded-full bg-sky-600 px-5 text-sm font-semibold text-white transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:opacity-60"
						>
							{isPosting ? "Đang đăng..." : "Xác nhận đăng bài"}
						</button>
						
						{/* Scheduled Posts Section */}
						{scheduledPosts.length > 0 && (
							<div className="mt-6 rounded-2xl border border-sky-200 bg-sky-50/50 p-4">
								<button
									type="button"
									onClick={() => setShowScheduled(!showScheduled)}
									className="flex w-full items-center justify-between text-sm font-semibold text-sky-700 hover:text-sky-800"
								>
									<span>📅 Bài đã lên lịch ({scheduledPosts.length})</span>
									<span className="text-lg">{showScheduled ? '▼' : '▶'}</span>
								</button>
								
								{showScheduled && (
									<div className="mt-4 space-y-3 max-h-96 overflow-y-auto">
										{scheduledPosts.map((post: any) => (
											<div key={post.id} className="rounded-xl border border-sky-200 bg-white p-4 shadow-sm">
												{/* Scheduled Time */}
												<div className="mb-3 flex items-center gap-2 text-sm font-semibold text-sky-700">
													<span>🕐</span>
													<span>
														{post.scheduled_publish_time 
															? new Date(post.scheduled_publish_time * 1000).toLocaleString("vi-VN", {
																	year: 'numeric',
																	month: '2-digit',
																	day: '2-digit',
																	hour: '2-digit',
																	minute: '2-digit'
															  })
															: "Chưa có thời gian"}
													</span>
												</div>
												
												{/* Caption */}
												{post.message && (
													<p className="text-sm text-gray-800 mb-3 line-clamp-3 whitespace-pre-wrap">
														{post.message}
													</p>
												)}
												
												{/* Image if exists */}
												{post.full_picture && (
													<img 
														src={post.full_picture} 
														alt="Scheduled post" 
														className="w-full h-40 object-cover rounded-lg"
													/>
												)}
											</div>
										))}
									</div>
								)}
							</div>
						)}
					</div>
				</section>
			</div>
		</div>
	);
}
